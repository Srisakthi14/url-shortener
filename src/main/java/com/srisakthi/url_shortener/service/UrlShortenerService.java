package com.srisakthi.url_shortener.service;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import com.srisakthi.url_shortener.cache.RedirectResolutionCache;
import com.srisakthi.url_shortener.cache.RedirectResolutionCache.CachedEntry;
import com.srisakthi.url_shortener.config.UrlShortenerProperties;
import com.srisakthi.url_shortener.dynamo.UrlMapping;
import com.srisakthi.url_shortener.dynamo.UrlMappingRepository;
import com.srisakthi.url_shortener.util.HourlyBucketFormat;
import com.srisakthi.url_shortener.web.api.AnalyticsResponse;
import com.srisakthi.url_shortener.web.api.BadRequestException;
import com.srisakthi.url_shortener.web.api.ConflictException;
import com.srisakthi.url_shortener.web.api.CreateUrlRequest;
import com.srisakthi.url_shortener.web.api.CreateUrlResponse;
import com.srisakthi.url_shortener.web.api.GoneException;
import com.srisakthi.url_shortener.web.api.NotFoundException;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@Service
public class UrlShortenerService {

	private final UrlMappingRepository repository;
	private final RedirectResolutionCache cache;
	private final ShortCodeGenerator codeGenerator;
	private final ShortCodeFormatValidator codeFormat;
	private final UrlValidationService validation;
	private final UrlShortenerProperties properties;
	private final MeterRegistry meterRegistry;
	private final Executor asyncExecutor;

	public UrlShortenerService(UrlMappingRepository repository, RedirectResolutionCache cache,
			ShortCodeGenerator codeGenerator, ShortCodeFormatValidator codeFormat, UrlValidationService validation,
			UrlShortenerProperties properties, MeterRegistry meterRegistry, Executor asyncExecutor) {
		this.repository = repository;
		this.cache = cache;
		this.codeGenerator = codeGenerator;
		this.codeFormat = codeFormat;
		this.validation = validation;
		this.properties = properties;
		this.meterRegistry = meterRegistry;
		this.asyncExecutor = asyncExecutor;
	}

	public CreateUrlResponse createUrl(CreateUrlRequest request) {
		return meterRegistry.timer("urlshortener.create.latency").record(() -> {
			String originalUrl = validation.validateAndNormalizeOriginalUrl(request.originalUrl());
			validation.validateTtlDays(request.ttlDays());

			int ttlDays = request.ttlDays() != null
					? request.ttlDays()
					: properties.getUrls().getDefaultTtlDays();
			long now = System.currentTimeMillis();
			long expiresAtMillis = now + ttlDays * 86_400_000L;
			long ttlEpochSeconds = expiresAtMillis / 1000;

			String custom = request.customAlias();
			if (custom != null && !custom.isBlank()) {
				String code = custom.trim().toLowerCase(Locale.ROOT);
				if (!codeFormat.isValid(code)) {
					throw new BadRequestException("customAlias must be exactly 7 characters [a-z0-9]");
				}
				UrlMapping mapping = UrlMapping.newMapping(code, originalUrl, now, expiresAtMillis, ttlEpochSeconds, true);
				try {
					repository.createNew(mapping);
				}
				catch (ConditionalCheckFailedException e) {
					throw new ConflictException("short code already exists");
				}
				cache.evictOrOverwriteOnCreate(code);
				cache.putHit(code, originalUrl, expiresAtMillis);
				return CreateUrlResponse.from(mapping);
			}

			for (int attempt = 0; attempt < codeGenerator.maxRandomRetries(); attempt++) {
				String code = codeGenerator.randomCode();
				UrlMapping mapping = UrlMapping.newMapping(code, originalUrl, now, expiresAtMillis, ttlEpochSeconds, false);
				try {
					repository.createNew(mapping);
					cache.evictOrOverwriteOnCreate(code);
					cache.putHit(code, originalUrl, expiresAtMillis);
					return CreateUrlResponse.from(mapping);
				}
				catch (ConditionalCheckFailedException ignored) {
					// collision; retry
				}
			}
			throw new IllegalStateException("exhausted short code generation retries");
		});
	}

	public URI resolveRedirect(String rawShortCode) {
		return meterRegistry.timer("urlshortener.redirect.latency").record(() -> {
			String shortCode = rawShortCode.toLowerCase(Locale.ROOT);
			if (!codeFormat.isValid(shortCode)) {
				throw new BadRequestException("invalid short code");
			}
			long now = System.currentTimeMillis();
			Optional<CachedEntry> cached = cache.get(shortCode);
			if (cached.isPresent()) {
				CachedEntry e = cached.get();
				if (e.negativeCache()) {
					throw new NotFoundException();
				}
				if (now > e.expiresAtMillis()) {
					throw new GoneException();
				}
				meterRegistry.counter("urlshortener.redirect.cache", "result", "hit").increment();
				recordRedirectAnalytics(shortCode, now);
				return URI.create(e.originalUrl());
			}

			meterRegistry.counter("urlshortener.redirect.cache", "result", "miss").increment();
			UrlMapping mapping = repository.findByShortCode(shortCode);
			if (mapping == null) {
				cache.putNegative(shortCode);
				throw new NotFoundException();
			}
			if (now > mapping.getExpiresAtMillis()) {
				throw new GoneException();
			}
			cache.putHit(shortCode, mapping.getOriginalUrl(), mapping.getExpiresAtMillis());
			recordRedirectAnalytics(shortCode, now);
			return URI.create(mapping.getOriginalUrl());
		});
	}

	private void recordRedirectAnalytics(String shortCode, long nowMillis) {
		String hourKey = HourlyBucketFormat.hourKeyUtc(Instant.ofEpochMilli(nowMillis));
		repository.incrementRedirectAnalytics(shortCode, hourKey, nowMillis);
		asyncExecutor.execute(() -> repository.compactHourlyBuckets(shortCode, nowMillis));
	}

	public AnalyticsResponse analytics(String rawShortCode) {
		return meterRegistry.timer("urlshortener.analytics.latency").record(() -> {
			String shortCode = rawShortCode.toLowerCase(Locale.ROOT);
			if (!codeFormat.isValid(shortCode)) {
				throw new BadRequestException("invalid short code");
			}
			long now = System.currentTimeMillis();
			UrlMapping mapping = repository.findByShortCode(shortCode);
			if (mapping == null) {
				throw new NotFoundException();
			}
			if (now > mapping.getExpiresAtMillis()) {
				throw new GoneException();
			}
			String cutoff = HourlyBucketFormat.cutoffHourKeyUtc(Instant.ofEpochMilli(now),
					UrlMappingRepository.HOURLY_RETENTION_DAYS);
			Map<String, Long> trimmed = HourlyBucketFormat.trimOlderThan(
					mapping.getHourlyBuckets() != null ? mapping.getHourlyBuckets() : Map.of(), cutoff);
			Map<String, Long> safeMap = new HashMap<>(trimmed);
			Long last = mapping.getLastAccessedAtMillis();
			Instant lastAccess = last != null ? Instant.ofEpochMilli(last) : null;
			return new AnalyticsResponse(
					shortCode,
					mapping.getTotalClicks() != null ? mapping.getTotalClicks() : 0L,
					lastAccess,
					safeMap);
		});
	}

}

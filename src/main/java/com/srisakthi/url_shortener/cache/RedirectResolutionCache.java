package com.srisakthi.url_shortener.cache;

import java.time.Duration;
import java.util.Optional;

import com.srisakthi.url_shortener.config.UrlShortenerProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Read-through cache for redirect metadata. If Redis is unavailable, operations degrade gracefully
 * (read returns empty; writes are skipped) so DynamoDB remains the source of truth.
 */
@Service
public class RedirectResolutionCache {

	private static final Logger log = LoggerFactory.getLogger(RedirectResolutionCache.class);
	private static final char SEP = '\u0001';

	private final StringRedisTemplate redis;
	private final UrlShortenerProperties properties;

	public RedirectResolutionCache(StringRedisTemplate redis, UrlShortenerProperties properties) {
		this.redis = redis;
		this.properties = properties;
	}

	public Optional<CachedEntry> get(String shortCode) {
		try {
			String key = dataKey(shortCode);
			String raw = redis.opsForValue().get(key);
			if (raw == null) {
				return Optional.empty();
			}
			if (MISS_MARKER.equals(raw)) {
				return Optional.of(CachedEntry.miss());
			}
			return Optional.of(parseHit(raw));
		}
		catch (Exception e) {
			log.warn("redis get failed for {}: {}", shortCode, e.toString());
			return Optional.empty();
		}
	}

	public void putHit(String shortCode, String originalUrl, long expiresAtMillis) {
		try {
			String payload = encodeHit(originalUrl, expiresAtMillis);
			Duration ttl = redisTtlUntil(expiresAtMillis);
			redis.opsForValue().set(dataKey(shortCode), payload, ttl);
		}
		catch (Exception e) {
			log.warn("redis put failed for {}: {}", shortCode, e.toString());
		}
	}

	public void putNegative(String shortCode) {
		try {
			redis.opsForValue().set(dataKey(shortCode), MISS_MARKER, properties.getCache().getNegativeTtl());
		}
		catch (Exception e) {
			log.warn("redis negative put failed for {}: {}", shortCode, e.toString());
		}
	}

	public void evictOrOverwriteOnCreate(String shortCode) {
		try {
			redis.delete(dataKey(shortCode));
		}
		catch (Exception e) {
			log.warn("redis delete failed for {}: {}", shortCode, e.toString());
		}
	}

	private String dataKey(String shortCode) {
		return properties.getCache().getKeyPrefix() + shortCode;
	}

	private Duration redisTtlUntil(long expiresAtMillis) {
		long now = System.currentTimeMillis();
		long remainingMs = expiresAtMillis - now;
		Duration cap = properties.getCache().getTtl();
		if (remainingMs <= 0) {
			return Duration.ofSeconds(1);
		}
		long capMs = cap.toMillis();
		long use = Math.min(remainingMs, capMs);
		Duration natural = Duration.ofMillis(Math.max(use, 1000));
		return natural;
	}

	private static String encodeHit(String originalUrl, long expiresAtMillis) {
		return expiresAtMillis + String.valueOf(SEP) + originalUrl;
	}

	private static CachedEntry parseHit(String raw) {
		int i = raw.indexOf(SEP);
		if (i < 1) {
			throw new IllegalArgumentException("bad cache payload");
		}
		long exp = Long.parseLong(raw.substring(0, i));
		String url = raw.substring(i + 1);
		return CachedEntry.hit(url, exp);
	}

	private static final String MISS_MARKER = "__miss__";

	public record CachedEntry(boolean negativeCache, String originalUrl, long expiresAtMillis) {
		public static CachedEntry miss() {
			return new CachedEntry(true, null, 0);
		}

		public static CachedEntry hit(String originalUrl, long expiresAtMillis) {
			return new CachedEntry(false, originalUrl, expiresAtMillis);
		}
	}

}

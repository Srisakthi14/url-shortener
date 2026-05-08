package com.srisakthi.url_shortener.service;

import java.net.URI;
import java.util.Locale;

import com.srisakthi.url_shortener.config.UrlShortenerProperties;
import com.srisakthi.url_shortener.web.api.BadRequestException;

import org.springframework.stereotype.Service;

@Service
public class UrlValidationService {

	private final UrlShortenerProperties properties;

	public UrlValidationService(UrlShortenerProperties properties) {
		this.properties = properties;
	}

	public String validateAndNormalizeOriginalUrl(String originalUrl) {
		if (originalUrl == null || originalUrl.isBlank()) {
			throw new BadRequestException("originalUrl is required");
		}
		if (originalUrl.length() > properties.getUrls().getMaxLength()) {
			throw new BadRequestException("originalUrl exceeds max length");
		}
		URI uri;
		try {
			uri = URI.create(originalUrl.trim());
		}
		catch (IllegalArgumentException e) {
			throw new BadRequestException("invalid URL");
		}
		String scheme = uri.getScheme();
		if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
			throw new BadRequestException("URL must use http or https");
		}
		String host = uri.getHost();
		if (host == null || host.isBlank()) {
			throw new BadRequestException("URL must include a host");
		}
		String hostLower = host.toLowerCase(Locale.ROOT);
		for (String blocked : properties.getPolicy().getBlockedDomains()) {
			if (blocked == null || blocked.isBlank()) {
				continue;
			}
			String b = blocked.trim().toLowerCase(Locale.ROOT);
			if (hostLower.equals(b) || hostLower.endsWith("." + b)) {
				throw new BadRequestException("blocked domain");
			}
		}
		return originalUrl.trim();
	}

	public void validateTtlDays(Integer ttlDays) {
		if (ttlDays == null) {
			return;
		}
		if (ttlDays < 1 || ttlDays > properties.getUrls().getMaxTtlDays()) {
			throw new BadRequestException("ttlDays must be between 1 and " + properties.getUrls().getMaxTtlDays());
		}
	}

}

package com.srisakthi.url_shortener.web.api;

import java.time.Instant;

import com.srisakthi.url_shortener.dynamo.UrlMapping;

public record CreateUrlResponse(
		String shortCode,
		String originalUrl,
		Instant createdAt,
		Instant expiresAt
) {
	public static CreateUrlResponse from(UrlMapping mapping) {
		return new CreateUrlResponse(
				mapping.getShortCode(),
				mapping.getOriginalUrl(),
				Instant.ofEpochMilli(mapping.getCreatedAtMillis()),
				Instant.ofEpochMilli(mapping.getExpiresAtMillis()));
	}
}

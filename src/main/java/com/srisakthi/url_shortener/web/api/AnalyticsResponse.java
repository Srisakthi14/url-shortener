package com.srisakthi.url_shortener.web.api;

import java.time.Instant;
import java.util.Map;

public record AnalyticsResponse(
		String shortCode,
		long totalClicks,
		Instant lastAccessedAt,
		Map<String, Long> hourlyBuckets
) {
}

package com.srisakthi.url_shortener;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Task 8.3: validating p95 redirect latency at ~5k RPS requires infrastructure (ALB, DynamoDB,
 * ElastiCache) and tools such as {@code hey}, {@code wrk}, or load testing in CI. Run against a
 * staging cluster and watch Micrometer timer {@code urlshortener.redirect.latency} percentiles.
 */
@Disabled("Manual / staging load test; see class javadoc")
class LoadTestHarnessPlaceholderIT {

	@Test
	void placeholder() {
	}
}

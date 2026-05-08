package com.srisakthi.url_shortener.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class HourlyBucketFormatTest {

	@Test
	void hourKeyUtcFormatsInUtc() {
		Instant t = Instant.parse("2026-05-08T15:30:00Z");
		assertThat(HourlyBucketFormat.hourKeyUtc(t)).isEqualTo("2026050815");
	}

	@Test
	void trimInPlaceDropsOlderBuckets() {
		Map<String, Long> m = new HashMap<>();
		m.put("2026050101", 1L);
		m.put("2026050812", 5L);
		HourlyBucketFormat.trimInPlace(m, "2026050500");
		assertThat(m).containsKey("2026050812");
		assertThat(m).doesNotContainKey("2026050101");
	}
}

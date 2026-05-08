package com.srisakthi.url_shortener.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class HourlyBucketFormat {

	private static final DateTimeFormatter HOUR_KEY = DateTimeFormatter.ofPattern("yyyyMMddHH").withZone(ZoneOffset.UTC);

	private HourlyBucketFormat() {
	}

	public static String hourKeyUtc(Instant instant) {
		return HOUR_KEY.format(instant);
	}

	/**
	 * Minimum hour key (UTC yyyyMMddHH) to retain for a rolling window of {@code retentionDays}.
	 */
	public static String cutoffHourKeyUtc(Instant now, int retentionDays) {
		ZonedDateTime z = ZonedDateTime.ofInstant(now, ZoneOffset.UTC).truncatedTo(ChronoUnit.HOURS);
		ZonedDateTime cutoff = z.minusDays(retentionDays);
		return HOUR_KEY.format(cutoff.toInstant());
	}

	public static Map<String, Long> trimOlderThan(Map<String, Long> buckets, String cutoffHourKeyInclusive) {
		if (buckets == null || buckets.isEmpty()) {
			return buckets == null ? Map.of() : Map.copyOf(buckets);
		}
		Map<String, Long> out = new HashMap<>();
		for (Map.Entry<String, Long> e : buckets.entrySet()) {
			if (e.getKey().compareTo(cutoffHourKeyInclusive) >= 0) {
				out.put(e.getKey(), e.getValue());
			}
		}
		return out;
	}

	/**
	 * Remove keys strictly older than cutoff (lexicographic yyyyMMddHH works for chronological order).
	 */
	public static void trimInPlace(Map<String, Long> buckets, String cutoffHourKeyInclusive) {
		if (buckets == null || buckets.isEmpty()) {
			return;
		}
		for (Iterator<String> it = buckets.keySet().iterator(); it.hasNext(); ) {
			String k = it.next();
			if (k.compareTo(cutoffHourKeyInclusive) < 0) {
				it.remove();
			}
		}
	}

}

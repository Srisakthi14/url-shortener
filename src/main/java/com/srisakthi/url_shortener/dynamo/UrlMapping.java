package com.srisakthi.url_shortener.dynamo;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class UrlMapping {

	private String shortCode;
	private String originalUrl;
	private Long createdAtMillis;
	private Long expiresAtMillis;
	private Long totalClicks;
	private Long lastAccessedAtMillis;
	private Map<String, Long> hourlyBuckets;
	private Boolean isCustomAlias;
	private Long ttl;

	@DynamoDbPartitionKey
	public String getShortCode() {
		return shortCode;
	}

	public void setShortCode(String shortCode) {
		this.shortCode = shortCode;
	}

	public String getOriginalUrl() {
		return originalUrl;
	}

	public void setOriginalUrl(String originalUrl) {
		this.originalUrl = originalUrl;
	}

	public Long getCreatedAtMillis() {
		return createdAtMillis;
	}

	public void setCreatedAtMillis(Long createdAtMillis) {
		this.createdAtMillis = createdAtMillis;
	}

	public Long getExpiresAtMillis() {
		return expiresAtMillis;
	}

	public void setExpiresAtMillis(Long expiresAtMillis) {
		this.expiresAtMillis = expiresAtMillis;
	}

	public Long getTotalClicks() {
		return totalClicks;
	}

	public void setTotalClicks(Long totalClicks) {
		this.totalClicks = totalClicks;
	}

	public Long getLastAccessedAtMillis() {
		return lastAccessedAtMillis;
	}

	public void setLastAccessedAtMillis(Long lastAccessedAtMillis) {
		this.lastAccessedAtMillis = lastAccessedAtMillis;
	}

	public Map<String, Long> getHourlyBuckets() {
		return hourlyBuckets;
	}

	public void setHourlyBuckets(Map<String, Long> hourlyBuckets) {
		this.hourlyBuckets = hourlyBuckets;
	}

	public Boolean getIsCustomAlias() {
		return isCustomAlias;
	}

	public void setIsCustomAlias(Boolean customAlias) {
		isCustomAlias = customAlias;
	}

	@DynamoDbAttribute("ttl")
	public Long getTtl() {
		return ttl;
	}

	public void setTtl(Long ttl) {
		this.ttl = ttl;
	}

	public static UrlMapping newMapping(String shortCode, String originalUrl, long createdAtMillis,
			long expiresAtMillis, long ttlEpochSeconds, boolean customAlias) {
		UrlMapping m = new UrlMapping();
		m.setShortCode(shortCode);
		m.setOriginalUrl(originalUrl);
		m.setCreatedAtMillis(createdAtMillis);
		m.setExpiresAtMillis(expiresAtMillis);
		m.setTotalClicks(0L);
		m.setLastAccessedAtMillis(null);
		m.setHourlyBuckets(new HashMap<>());
		m.setIsCustomAlias(customAlias);
		m.setTtl(ttlEpochSeconds);
		return m;
	}

}

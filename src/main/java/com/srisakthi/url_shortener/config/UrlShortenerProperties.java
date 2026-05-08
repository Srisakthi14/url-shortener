package com.srisakthi.url_shortener.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "url-shortener")
public class UrlShortenerProperties {

	private Aws aws = new Aws();
	private DynamoDb dynamodb = new DynamoDb();
	private Cache cache = new Cache();
	private Urls urls = new Urls();
	private Policy policy = new Policy();
	private RateLimit rateLimit = new RateLimit();
	private Code code = new Code();

	public Aws getAws() {
		return aws;
	}

	public void setAws(Aws aws) {
		this.aws = aws;
	}

	public DynamoDb getDynamodb() {
		return dynamodb;
	}

	public void setDynamodb(DynamoDb dynamodb) {
		this.dynamodb = dynamodb;
	}

	public Cache getCache() {
		return cache;
	}

	public void setCache(Cache cache) {
		this.cache = cache;
	}

	public Urls getUrls() {
		return urls;
	}

	public void setUrls(Urls urls) {
		this.urls = urls;
	}

	public Policy getPolicy() {
		return policy;
	}

	public void setPolicy(Policy policy) {
		this.policy = policy;
	}

	public RateLimit getRateLimit() {
		return rateLimit;
	}

	public void setRateLimit(RateLimit rateLimit) {
		this.rateLimit = rateLimit;
	}

	public Code getCode() {
		return code;
	}

	public void setCode(Code code) {
		this.code = code;
	}

	public static class Aws {

		private String region = "us-east-1";

		public String getRegion() {
			return region;
		}

		public void setRegion(String region) {
			this.region = region;
		}
	}

	public static class DynamoDb {

		private String endpoint = "";
		private String tableName = "url-shortener-mappings";

		public String getEndpoint() {
			return endpoint;
		}

		public void setEndpoint(String endpoint) {
			this.endpoint = endpoint;
		}

		public String getTableName() {
			return tableName;
		}

		public void setTableName(String tableName) {
			this.tableName = tableName;
		}
	}

	public static class Cache {

		private String keyPrefix = "url:";
		private Duration ttl = Duration.ofHours(1);
		private Duration negativeTtl = Duration.ofMinutes(1);

		public String getKeyPrefix() {
			return keyPrefix;
		}

		public void setKeyPrefix(String keyPrefix) {
			this.keyPrefix = keyPrefix;
		}

		public Duration getTtl() {
			return ttl;
		}

		public void setTtl(Duration ttl) {
			this.ttl = ttl;
		}

		public Duration getNegativeTtl() {
			return negativeTtl;
		}

		public void setNegativeTtl(Duration negativeTtl) {
			this.negativeTtl = negativeTtl;
		}
	}

	public static class Urls {

		private int maxLength = 2048;
		private int defaultTtlDays = 30;
		private int maxTtlDays = 365;

		public int getMaxLength() {
			return maxLength;
		}

		public void setMaxLength(int maxLength) {
			this.maxLength = maxLength;
		}

		public int getDefaultTtlDays() {
			return defaultTtlDays;
		}

		public void setDefaultTtlDays(int defaultTtlDays) {
			this.defaultTtlDays = defaultTtlDays;
		}

		public int getMaxTtlDays() {
			return maxTtlDays;
		}

		public void setMaxTtlDays(int maxTtlDays) {
			this.maxTtlDays = maxTtlDays;
		}
	}

	public static class Policy {

		private List<String> blockedDomains = new ArrayList<>();

		public List<String> getBlockedDomains() {
			return blockedDomains;
		}

		public void setBlockedDomains(List<String> blockedDomains) {
			this.blockedDomains = blockedDomains;
		}
	}

	public static class RateLimit {

		private long createCapacity = 120;
		private long createRefillMinutes = 1;
		private long redirectCapacity = 6000;
		private long redirectRefillMinutes = 1;

		public long getCreateCapacity() {
			return createCapacity;
		}

		public void setCreateCapacity(long createCapacity) {
			this.createCapacity = createCapacity;
		}

		public long getCreateRefillMinutes() {
			return createRefillMinutes;
		}

		public void setCreateRefillMinutes(long createRefillMinutes) {
			this.createRefillMinutes = createRefillMinutes;
		}

		public long getRedirectCapacity() {
			return redirectCapacity;
		}

		public void setRedirectCapacity(long redirectCapacity) {
			this.redirectCapacity = redirectCapacity;
		}

		public long getRedirectRefillMinutes() {
			return redirectRefillMinutes;
		}

		public void setRedirectRefillMinutes(long redirectRefillMinutes) {
			this.redirectRefillMinutes = redirectRefillMinutes;
		}
	}

	public static class Code {

		private int randomRetriesMax = 12;

		public int getRandomRetriesMax() {
			return randomRetriesMax;
		}

		public void setRandomRetriesMax(int randomRetriesMax) {
			this.randomRetriesMax = randomRetriesMax;
		}
	}

}

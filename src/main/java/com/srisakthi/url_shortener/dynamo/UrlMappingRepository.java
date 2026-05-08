package com.srisakthi.url_shortener.dynamo;

import java.util.HashMap;
import java.util.Map;

import com.srisakthi.url_shortener.config.UrlShortenerProperties;
import com.srisakthi.url_shortener.util.HourlyBucketFormat;

import org.springframework.stereotype.Repository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@Repository
public class UrlMappingRepository {

	public static final int HOURLY_RETENTION_DAYS = 30;

	private final DynamoDbTable<UrlMapping> table;
	private final DynamoDbClient rawClient;
	private final String tableName;

	public UrlMappingRepository(DynamoDbClient dynamoDbClient, DynamoDbEnhancedClient enhanced,
			UrlShortenerProperties properties) {
		this.rawClient = dynamoDbClient;
		this.tableName = properties.getDynamodb().getTableName();
		this.table = enhanced.table(tableName, TableSchema.fromBean(UrlMapping.class));
	}

	public void createNew(UrlMapping mapping) throws ConditionalCheckFailedException {
		table.putItem(PutItemEnhancedRequest.builder(UrlMapping.class)
				.item(mapping)
				.conditionExpression(
						Expression.builder().expression("attribute_not_exists(shortCode)").build())
				.build());
	}

	public UrlMapping findByShortCode(String shortCode) {
		return table.getItem(Key.builder().partitionValue(shortCode).build());
	}

	public void incrementRedirectAnalytics(String shortCode, String hourKeyUtc, long nowMillis) {
		String ue = "ADD totalClicks :one SET lastAccessedAtMillis = :ts, hourlyBuckets.#hk = if_not_exists(hourlyBuckets.#hk, :z) + :inc";
		UpdateItemRequest request = UpdateItemRequest.builder()
				.tableName(tableName)
				.key(Map.of("shortCode", AttributeValue.fromS(shortCode)))
				.updateExpression(ue)
				.expressionAttributeNames(Map.of("#hk", hourKeyUtc))
				.expressionAttributeValues(Map.of(
						":one", AttributeValue.fromN("1"),
						":ts", AttributeValue.fromN(Long.toString(nowMillis)),
						":z", AttributeValue.fromN("0"),
						":inc", AttributeValue.fromN("1")))
				.build();
		rawClient.updateItem(request);
	}

	public void replaceHourlyBuckets(String shortCode, Map<String, Long> hourlyBuckets) {
		rawClient.updateItem(UpdateItemRequest.builder()
				.tableName(tableName)
				.key(Map.of("shortCode", AttributeValue.fromS(shortCode)))
				.updateExpression("SET hourlyBuckets = :m")
				.expressionAttributeValues(Map.of(":m", toAttributeMap(hourlyBuckets)))
				.build());
	}

	public void compactHourlyBuckets(String shortCode, long nowMillis) {
		UrlMapping current = findByShortCode(shortCode);
		if (current == null || current.getHourlyBuckets() == null || current.getHourlyBuckets().isEmpty()) {
			return;
		}
		String cutoff = HourlyBucketFormat.cutoffHourKeyUtc(
				java.time.Instant.ofEpochMilli(nowMillis), HOURLY_RETENTION_DAYS);
		Map<String, Long> copy = new HashMap<>(current.getHourlyBuckets());
		HourlyBucketFormat.trimInPlace(copy, cutoff);
		if (copy.size() != current.getHourlyBuckets().size()) {
			replaceHourlyBuckets(shortCode, copy);
		}
	}

	private static AttributeValue toAttributeMap(Map<String, Long> map) {
		Map<String, AttributeValue> inner = new HashMap<>();
		map.forEach((k, v) -> inner.put(k, AttributeValue.fromN(v.toString())));
		return AttributeValue.fromM(inner);
	}

}

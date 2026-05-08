package com.srisakthi.url_shortener.health;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import com.srisakthi.url_shortener.config.UrlShortenerProperties;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

@Component
@ConditionalOnProperty(name = "url-shortener.health.dynamodb-enabled", havingValue = "true", matchIfMissing = true)
public class DynamoDbTableHealthIndicator implements HealthIndicator {

	private final DynamoDbClient dynamoDbClient;
	private final String tableName;

	public DynamoDbTableHealthIndicator(DynamoDbClient dynamoDbClient, UrlShortenerProperties properties) {
		this.dynamoDbClient = dynamoDbClient;
		this.tableName = properties.getDynamodb().getTableName();
	}

	@Override
	public Health health() {
		try {
			dynamoDbClient.describeTable(b -> b.tableName(tableName));
			return Health.up().withDetail("table", tableName).build();
		}
		catch (DynamoDbException e) {
			return Health.down(e).withDetail("table", tableName).build();
		}
	}

}

package com.srisakthi.url_shortener;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.srisakthi.url_shortener.cache.RedirectResolutionCache;
import com.srisakthi.url_shortener.dynamo.UrlMappingRepository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@SpringBootTest
@ActiveProfiles("test")
class UrlShortenerApplicationTests {

	@MockitoBean
	DynamoDbClient dynamoDbClient;

	@MockitoBean
	DynamoDbEnhancedClient dynamoDbEnhancedClient;

	@MockitoBean
	UrlMappingRepository urlMappingRepository;

	@MockitoBean
	RedirectResolutionCache redirectResolutionCache;

	@Test
	void contextLoads() {
	}
}

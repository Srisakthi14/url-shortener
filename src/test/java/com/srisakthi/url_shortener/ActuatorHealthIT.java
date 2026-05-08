package com.srisakthi.url_shortener;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.srisakthi.url_shortener.cache.RedirectResolutionCache;
import com.srisakthi.url_shortener.dynamo.UrlMappingRepository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActuatorHealthIT {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	DynamoDbClient dynamoDbClient;

	@MockitoBean
	DynamoDbEnhancedClient dynamoDbEnhancedClient;

	@MockitoBean
	UrlMappingRepository urlMappingRepository;

	@MockitoBean
	RedirectResolutionCache redirectResolutionCache;

	@Test
	void healthEndpointReturns200() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("\"status\":\"UP\"")));
	}
}

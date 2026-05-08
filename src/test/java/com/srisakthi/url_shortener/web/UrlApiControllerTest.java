package com.srisakthi.url_shortener.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.srisakthi.url_shortener.service.UrlShortenerService;
import com.srisakthi.url_shortener.web.api.CreateUrlRequest;
import com.srisakthi.url_shortener.web.api.CreateUrlResponse;

@WebMvcTest(controllers = UrlApiController.class)
@AutoConfigureMockMvc
class UrlApiControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UrlShortenerService urlShortenerService;

	@Test
	void createReturns201() throws Exception {
		when(urlShortenerService.createUrl(any(CreateUrlRequest.class))).thenReturn(new CreateUrlResponse(
				"abc12xy",
				"https://example.com/path",
				Instant.parse("2026-05-08T12:00:00Z"),
				Instant.parse("2026-06-07T12:00:00Z")));

		mockMvc.perform(post("/api/v1/urls")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"originalUrl\":\"https://example.com/path\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.shortCode").value("abc12xy"));
	}
}

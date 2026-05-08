package com.srisakthi.url_shortener.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.srisakthi.url_shortener.service.UrlShortenerService;
import com.srisakthi.url_shortener.web.api.AnalyticsResponse;
import com.srisakthi.url_shortener.web.api.CreateUrlRequest;
import com.srisakthi.url_shortener.web.api.CreateUrlResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlApiController {

	private final UrlShortenerService urlShortenerService;

	public UrlApiController(UrlShortenerService urlShortenerService) {
		this.urlShortenerService = urlShortenerService;
	}

	@PostMapping
	public ResponseEntity<CreateUrlResponse> create(@Valid @RequestBody CreateUrlRequest body) {
		CreateUrlResponse created = urlShortenerService.createUrl(body);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@GetMapping("/{shortCode}/analytics")
	public AnalyticsResponse analytics(@PathVariable String shortCode) {
		return urlShortenerService.analytics(shortCode);
	}

}

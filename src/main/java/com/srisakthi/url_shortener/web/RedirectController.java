package com.srisakthi.url_shortener.web;

import java.net.URI;

import com.srisakthi.url_shortener.service.UrlShortenerService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

	private final UrlShortenerService urlShortenerService;

	public RedirectController(UrlShortenerService urlShortenerService) {
		this.urlShortenerService = urlShortenerService;
	}

	@GetMapping("/{shortCode:[a-z0-9]{7}}")
	public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
		URI location = urlShortenerService.resolveRedirect(shortCode);
		return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, location.toASCIIString()).build();
	}

}

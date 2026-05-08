package com.srisakthi.url_shortener.service;

import java.security.SecureRandom;

import com.srisakthi.url_shortener.config.UrlShortenerProperties;

import org.springframework.stereotype.Service;

@Service
public class ShortCodeGenerator {

	private static final char[] ALPHANUM = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

	private final SecureRandom random = new SecureRandom();
	private final UrlShortenerProperties properties;

	public ShortCodeGenerator(UrlShortenerProperties properties) {
		this.properties = properties;
	}

	public String randomCode() {
		char[] buf = new char[7];
		for (int i = 0; i < 7; i++) {
			buf[i] = ALPHANUM[random.nextInt(ALPHANUM.length)];
		}
		return new String(buf);
	}

	public int maxRandomRetries() {
		return properties.getCode().getRandomRetriesMax();
	}

}

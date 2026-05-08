package com.srisakthi.url_shortener.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class ShortCodeFormatValidator {

	private static final Pattern CODE = Pattern.compile("^[a-z0-9]{7}$");

	public boolean isValid(String shortCode) {
		return shortCode != null && CODE.matcher(shortCode).matches();
	}

}

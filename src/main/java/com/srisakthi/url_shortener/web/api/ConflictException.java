package com.srisakthi.url_shortener.web.api;

public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}

}

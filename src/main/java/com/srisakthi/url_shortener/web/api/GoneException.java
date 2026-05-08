package com.srisakthi.url_shortener.web.api;

public class GoneException extends RuntimeException {

	public GoneException() {
		super("gone");
	}

}

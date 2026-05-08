package com.srisakthi.url_shortener.web.api;

public class NotFoundException extends RuntimeException {

	public NotFoundException() {
		super("not found");
	}

}

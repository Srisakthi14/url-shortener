package com.srisakthi.url_shortener.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ShortCodeFormatValidatorTest {

	private final ShortCodeFormatValidator validator = new ShortCodeFormatValidator();

	@Test
	void acceptsSevenLowercaseAlphanumeric() {
		assertThat(validator.isValid("abc12xy")).isTrue();
	}

	@Test
	void rejectsWrongLength() {
		assertThat(validator.isValid("abc12x")).isFalse();
		assertThat(validator.isValid("abc12xyz")).isFalse();
	}

	@Test
	void rejectsUppercaseAndPunctuation() {
		assertThat(validator.isValid("Abc12xy")).isFalse();
		assertThat(validator.isValid("abc12x-")).isFalse();
	}
}

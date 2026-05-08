package com.srisakthi.url_shortener.web.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateUrlRequest(
		@NotBlank String originalUrl,
		String customAlias,
		@Min(1) @Max(365) Integer ttlDays
) {

}

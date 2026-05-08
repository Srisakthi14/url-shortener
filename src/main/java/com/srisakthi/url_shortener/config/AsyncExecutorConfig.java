package com.srisakthi.url_shortener.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AsyncExecutorConfig {

	@Bean
	public Executor taskExecutor() {
		return Executors.newFixedThreadPool(16);
	}

}

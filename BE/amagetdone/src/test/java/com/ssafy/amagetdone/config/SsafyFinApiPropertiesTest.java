package com.ssafy.amagetdone.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ssafy.amagetdone.global.config.SsafyFinApiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class SsafyFinApiPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(TestConfiguration.class)
		.withPropertyValues(
			"ssafy.fin-api.base-url=https://finopenapi.ssafy.io/ssafy/api/v1",
			"ssafy.fin-api.api-key=test-api-key"
		);

	@Test
	void bindsSsafyFinancialApiProperties() {
		contextRunner.run(context -> {
			SsafyFinApiProperties properties = context.getBean(SsafyFinApiProperties.class);

			assertEquals("https://finopenapi.ssafy.io/ssafy/api/v1", properties.getBaseUrl());
			assertEquals("test-api-key", properties.getApiKey());
		});
	}

	@Configuration
	@EnableConfigurationProperties(SsafyFinApiProperties.class)
	static class TestConfiguration {
	}
}

package com.ssafy.amagetdone.domain.chatbot.infra.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.ssafy.amagetdone.domain.transaction.infra.client.MerchantInferenceApiProperties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

class ChatbotApiClientConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(TestConfiguration.class)
		.withPropertyValues("transaction.merchant-inference.base-url=http://localhost:8000");

	@Test
	void registersChatbotApiClientBean() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(ChatbotApiClient.class);
		});
	}

	@Configuration
	@Import(ChatbotApiClient.class)
	@EnableConfigurationProperties(MerchantInferenceApiProperties.class)
	static class TestConfiguration {

		@Bean
		WebClient.Builder webClientBuilder() {
			return WebClient.builder();
		}
	}
}

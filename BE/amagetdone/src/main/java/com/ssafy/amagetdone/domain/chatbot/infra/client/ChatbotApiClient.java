package com.ssafy.amagetdone.domain.chatbot.infra.client;

import java.time.Duration;

import com.ssafy.amagetdone.domain.chatbot.api.dto.ChatbotMessageRequest;
import com.ssafy.amagetdone.domain.chatbot.exception.ChatbotErrorCode;
import com.ssafy.amagetdone.domain.chatbot.exception.ChatbotRelayException;
import com.ssafy.amagetdone.domain.transaction.infra.client.MerchantInferenceApiProperties;

import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

@Component
public class ChatbotApiClient {

	private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(60);
	private static final String USER_ID_HEADER = "X-User-Id";
	private static final String MESSAGES_PATH = "/v1/chatbot/messages";
	private static final String STREAM_MESSAGES_PATH = "/v1/chatbot/messages/stream";
	private static final String CONVERSATIONS_PATH = "/v1/chatbot/conversations/{conversationId}";

	private final WebClient webClient;
	private final String baseUrl;
	private final Duration readTimeout;

	@Autowired
	public ChatbotApiClient(WebClient.Builder webClientBuilder, MerchantInferenceApiProperties properties) {
		this(webClientBuilder, properties, DEFAULT_READ_TIMEOUT);
	}

	ChatbotApiClient(
		WebClient.Builder webClientBuilder,
		MerchantInferenceApiProperties properties,
		Duration readTimeout
	) {
		this.baseUrl = trimTrailingSlash(properties.getBaseUrl());
		this.readTimeout = readTimeout;
		this.webClient = webClientBuilder
			.baseUrl(baseUrl)
			.build();
	}

	public ChatbotRelayResult postMessage(int userId, ChatbotMessageRequest request) {
		validateConfiguration();
		return execute(() -> webClient.post()
			.uri(MESSAGES_PATH)
			.contentType(MediaType.APPLICATION_JSON)
			.header(USER_ID_HEADER, String.valueOf(userId))
			.bodyValue(request)
			.exchangeToMono(this::toJsonResult)
			.block(readTimeout));
	}

	public ChatbotRelayResult getConversation(int userId, String conversationId) {
		validateConfiguration();
		return execute(() -> webClient.get()
			.uri(CONVERSATIONS_PATH, conversationId)
			.header(USER_ID_HEADER, String.valueOf(userId))
			.exchangeToMono(this::toJsonResult)
			.block(readTimeout));
	}

	public ChatbotRelayResult getLatestConversation(int userId) {
		validateConfiguration();
		return execute(() -> webClient.get()
			.uri("/v1/chatbot/conversations/latest")
			.header(USER_ID_HEADER, String.valueOf(userId))
			.exchangeToMono(this::toJsonResult)
			.block(readTimeout));
	}

	public ChatbotRelayResult streamMessage(int userId, ChatbotMessageRequest request) {
		validateConfiguration();
		ClientResponse response = execute(() -> webClient.post()
			.uri(STREAM_MESSAGES_PATH)
			.contentType(MediaType.APPLICATION_JSON)
			.accept(MediaType.TEXT_EVENT_STREAM)
			.header(USER_ID_HEADER, String.valueOf(userId))
			.bodyValue(request)
			.exchange()
			.block(readTimeout));

		MediaType contentType = response.headers().contentType().orElse(MediaType.TEXT_EVENT_STREAM);
		HttpStatus status = HttpStatus.valueOf(response.statusCode().value());
		return ChatbotRelayResult.stream(status, contentType, response.bodyToFlux(DataBuffer.class));
	}

	private Mono<ChatbotRelayResult> toJsonResult(ClientResponse response) {
		MediaType contentType = response.headers().contentType().orElse(MediaType.APPLICATION_JSON);
		HttpStatus status = HttpStatus.valueOf(response.statusCode().value());
		return response.bodyToMono(String.class)
			.defaultIfEmpty("")
			.map(body -> ChatbotRelayResult.json(status, contentType, body));
	}

	private String trimTrailingSlash(String baseUrl) {
		if (baseUrl == null || baseUrl.isBlank()) {
			return "";
		}
		return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
	}

	private void validateConfiguration() {
		if (baseUrl.isBlank()) {
			throw new ChatbotRelayException(ChatbotErrorCode.CHATBOT_UPSTREAM_UNAVAILABLE);
		}
	}

	private <T> T execute(OutboundCall<T> outboundCall) {
		try {
			return outboundCall.run();
		} catch (ChatbotRelayException exception) {
			throw exception;
		} catch (WebClientException | IllegalStateException exception) {
			throw new ChatbotRelayException(ChatbotErrorCode.CHATBOT_UPSTREAM_UNAVAILABLE, exception);
		}
	}

	@FunctionalInterface
	private interface OutboundCall<T> {
		T run();
	}
}

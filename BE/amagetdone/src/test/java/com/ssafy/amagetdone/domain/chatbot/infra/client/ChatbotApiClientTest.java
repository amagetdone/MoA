package com.ssafy.amagetdone.domain.chatbot.infra.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.ssafy.amagetdone.domain.chatbot.api.dto.ChatbotMessageRequest;
import com.ssafy.amagetdone.domain.chatbot.exception.ChatbotErrorCode;
import com.ssafy.amagetdone.domain.chatbot.exception.ChatbotRelayException;
import com.ssafy.amagetdone.domain.transaction.infra.client.MerchantInferenceApiProperties;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

class ChatbotApiClientTest {

	private MockWebServer mockWebServer;
	private ChatbotApiClient chatbotApiClient;

	@BeforeEach
	void setUp() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();

		MerchantInferenceApiProperties properties = new MerchantInferenceApiProperties();
		properties.setBaseUrl(mockWebServer.url("/").toString());

		chatbotApiClient = new ChatbotApiClient(
			WebClient.builder(),
			properties,
			Duration.ofSeconds(5)
		);
	}

	@AfterEach
	void tearDown() throws IOException {
		mockWebServer.shutdown();
	}

	@Test
	void postMessageSendsOnlyXUserIdHeaderToUpstream() throws Exception {
		mockWebServer.enqueue(new MockResponse()
			.setResponseCode(200)
			.addHeader("Content-Type", "application/json")
			.setBody("""
				{"conversation_id":"conv-1","answer":"ok","warnings":[],"tool_summaries":[]}
				"""));

		ChatbotRelayResult result = chatbotApiClient.postMessage(
			7,
			new ChatbotMessageRequest("budget status?", "conv-1")
		);

		RecordedRequest request = mockWebServer.takeRequest();
		assertThat(request.getPath()).isEqualTo("/v1/chatbot/messages");
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getHeader("X-User-Id")).isEqualTo("7");
		assertThat(request.getHeader("Authorization")).isNull();
		assertThat(request.getBody().readUtf8())
			.isEqualTo("{\"message\":\"budget status?\",\"conversation_id\":\"conv-1\"}");
		assertThat(result.status()).isEqualTo(HttpStatus.OK);
		assertThat(result.contentType()).isEqualTo(MediaType.APPLICATION_JSON);
		assertThat(result.body()).contains("\"conversation_id\":\"conv-1\"");
	}

	@Test
	void getConversationPreservesGoneResponse() throws Exception {
		mockWebServer.enqueue(new MockResponse()
			.setResponseCode(410)
			.addHeader("Content-Type", "application/json")
			.setBody("""
				{"detail":"conversation expired"}
				"""));

		ChatbotRelayResult result = chatbotApiClient.getConversation(7, "conv-expired");

		RecordedRequest request = mockWebServer.takeRequest();
		assertThat(request.getPath()).isEqualTo("/v1/chatbot/conversations/conv-expired");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getHeader("X-User-Id")).isEqualTo("7");
		assertThat(result.status()).isEqualTo(HttpStatus.GONE);
		assertThat(result.body()).contains("conversation expired");
	}

	@Test
	void getLatestConversationPreservesNotFoundResponse() throws Exception {
		mockWebServer.enqueue(new MockResponse()
			.setResponseCode(404)
			.addHeader("Content-Type", "application/json")
			.setBody("""
				{"detail":"active conversation not found"}
				"""));

		ChatbotRelayResult result = chatbotApiClient.getLatestConversation(7);

		RecordedRequest request = mockWebServer.takeRequest();
		assertThat(request.getPath()).isEqualTo("/v1/chatbot/conversations/latest");
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getHeader("X-User-Id")).isEqualTo("7");
		assertThat(result.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(result.body()).contains("active conversation not found");
	}

	@Test
	void streamMessagePreservesUpstreamSseFrames() throws Exception {
		mockWebServer.enqueue(new MockResponse()
			.setResponseCode(200)
			.addHeader("Content-Type", "text/event-stream")
			.setChunkedBody("""
				event: status
				data: {"message":"routing"}

				event: token
				data: {"text":"## Budget"}

				event: done
				data: {"conversation_id":"conv-123"}

				""", 16));

		ChatbotRelayResult result = chatbotApiClient.streamMessage(
			7,
			new ChatbotMessageRequest("budget status?", null)
		);

		String collected = DataBufferUtils.join(result.streamBody())
			.map(buffer -> {
				byte[] bytes = new byte[buffer.readableByteCount()];
				buffer.read(bytes);
				DataBufferUtils.release(buffer);
				return new String(bytes, StandardCharsets.UTF_8);
			})
			.block(Duration.ofSeconds(3));

		RecordedRequest request = mockWebServer.takeRequest();
		assertThat(request.getPath()).isEqualTo("/v1/chatbot/messages/stream");
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getHeader("X-User-Id")).isEqualTo("7");
		assertThat(result.status()).isEqualTo(HttpStatus.OK);
		assertThat(result.contentType()).isEqualTo(MediaType.TEXT_EVENT_STREAM);
		assertThat(collected).contains("event: status");
		assertThat(collected).contains("event: token");
		assertThat(collected).contains("event: done");
	}

	@Test
	void postMessageThrowsRelayExceptionWhenUpstreamUnavailable() throws IOException {
		mockWebServer.shutdown();

		assertThatThrownBy(() -> chatbotApiClient.postMessage(
			7,
			new ChatbotMessageRequest("budget status?", null)
		)).isInstanceOf(ChatbotRelayException.class);
	}

	@Test
	void postMessageThrowsRelayExceptionWhenSharedBaseUrlIsBlank() {
		MerchantInferenceApiProperties properties = new MerchantInferenceApiProperties();
		ChatbotApiClient client = new ChatbotApiClient(
			WebClient.builder(),
			properties,
			Duration.ofSeconds(5)
		);

		assertThatThrownBy(() -> client.postMessage(
			7,
			new ChatbotMessageRequest("budget status?", null)
		))
			.isInstanceOfSatisfying(ChatbotRelayException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ChatbotErrorCode.CHATBOT_UPSTREAM_UNAVAILABLE)
			);
	}
}

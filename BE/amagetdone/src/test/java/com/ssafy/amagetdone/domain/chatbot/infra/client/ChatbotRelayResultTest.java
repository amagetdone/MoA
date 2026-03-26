package com.ssafy.amagetdone.domain.chatbot.infra.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import reactor.core.publisher.Flux;

class ChatbotRelayResultTest {

	@Test
	void streamingResponseBodyFlushesEachChunk() throws Exception {
		ChatbotRelayResult relayResult = ChatbotRelayResult.stream(
			HttpStatus.OK,
			MediaType.TEXT_EVENT_STREAM,
			Flux.just(
				DefaultDataBufferFactory.sharedInstance.wrap("event: token\n".getBytes(StandardCharsets.UTF_8)),
				DefaultDataBufferFactory.sharedInstance.wrap("data: {\"text\":\"a\"}\n\n".getBytes(StandardCharsets.UTF_8))
			)
		);

		ResponseEntity<StreamingResponseBody> response = relayResult.toStreamResponseEntity();
		CountingOutputStream outputStream = new CountingOutputStream();

		response.getBody().writeTo(outputStream);

		assertThat(outputStream.flushCount()).isEqualTo(2);
		assertThat(outputStream.contentAsString(StandardCharsets.UTF_8))
			.isEqualTo("event: token\ndata: {\"text\":\"a\"}\n\n");
	}

	private static final class CountingOutputStream extends ByteArrayOutputStream {

		private int flushCount;

		@Override
		public void flush() throws IOException {
			flushCount++;
			super.flush();
		}

		int flushCount() {
			return flushCount;
		}

		String contentAsString(java.nio.charset.Charset charset) {
			return new String(toByteArray(), charset);
		}
	}
}

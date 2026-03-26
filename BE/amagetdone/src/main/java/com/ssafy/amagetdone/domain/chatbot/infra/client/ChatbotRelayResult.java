package com.ssafy.amagetdone.domain.chatbot.infra.client;

import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public record ChatbotRelayResult(
	HttpStatus status,
	MediaType contentType,
	String body,
	Flux<DataBuffer> streamBody
) {

	public static ChatbotRelayResult json(HttpStatus status, MediaType contentType, String body) {
		return new ChatbotRelayResult(status, contentType, body, null);
	}

	public static ChatbotRelayResult stream(HttpStatus status, MediaType contentType, Flux<DataBuffer> streamBody) {
		return new ChatbotRelayResult(status, contentType, null, streamBody);
	}

	public ResponseEntity<String> toStringResponseEntity() {
		HttpHeaders headers = new HttpHeaders();
		if (contentType != null) {
			headers.setContentType(contentType);
		}
		return new ResponseEntity<>(body == null ? "" : body, headers, status);
	}

	public ResponseEntity<Flux<DataBuffer>> toStreamResponseEntity() {
		HttpHeaders headers = new HttpHeaders();
		if (contentType != null) {
			headers.setContentType(contentType);
		}
		headers.setCacheControl("no-cache");
		headers.add("X-Accel-Buffering", "no");

		Flux<DataBuffer> responseBody = streamBody != null
			? streamBody
			: Flux.just(DefaultDataBufferFactory.sharedInstance.wrap((body == null ? "" : body).getBytes()));

		return new ResponseEntity<>(responseBody, headers, status);
	}
}

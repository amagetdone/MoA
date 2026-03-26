package com.ssafy.amagetdone.domain.chatbot.api.controller;

import com.ssafy.amagetdone.domain.chatbot.api.dto.ChatbotMessageRequest;
import com.ssafy.amagetdone.domain.chatbot.application.service.ChatbotRelayService;
import com.ssafy.amagetdone.global.docs.controller.ChatbotDocsController;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/chatbot")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ChatbotController implements ChatbotDocsController {

	private final ChatbotRelayService chatbotRelayService;

	@PostMapping("/messages")
	public ResponseEntity<String> postMessage(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@RequestBody ChatbotMessageRequest request
	) {
		return chatbotRelayService.relayMessage(authenticatedMember.userId(), request).toStringResponseEntity();
	}

	@PostMapping(path = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public ResponseEntity<Flux<DataBuffer>> streamMessage(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@RequestBody ChatbotMessageRequest request
	) {
		return chatbotRelayService.relayStream(authenticatedMember.userId(), request).toStreamResponseEntity();
	}

	@GetMapping("/conversations/latest")
	public ResponseEntity<String> getLatestConversation(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember
	) {
		return chatbotRelayService.relayLatestConversation(authenticatedMember.userId())
			.toStringResponseEntity();
	}

	@GetMapping("/conversations/{conversationId}")
	public ResponseEntity<String> getConversation(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@PathVariable String conversationId
	) {
		return chatbotRelayService.relayConversation(authenticatedMember.userId(), conversationId)
			.toStringResponseEntity();
	}
}

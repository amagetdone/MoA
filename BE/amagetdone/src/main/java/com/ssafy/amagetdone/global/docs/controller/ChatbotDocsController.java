package com.ssafy.amagetdone.global.docs.controller;

import com.ssafy.amagetdone.domain.chatbot.api.dto.ChatbotMessageRequest;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Tag(name = "Chatbot", description = "Chatbot relay API")
public interface ChatbotDocsController {

	@Operation(summary = "Send chatbot message", description = "Relays a chatbot message request to the inference server.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Chatbot response relayed"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Conversation not found", content = @Content),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "Conversation expired", content = @Content),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Invalid request", content = @Content),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Upstream relay failed", content = @Content),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Chatbot unavailable", content = @Content)
	})
	ResponseEntity<String> postMessage(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "Chatbot message request",
			content = @Content(schema = @Schema(implementation = ChatbotMessageRequest.class))
		)
		ChatbotMessageRequest request
	);

	@Operation(summary = "Stream chatbot message", description = "Relays chatbot SSE output without rebuilding the stream.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Chatbot stream relayed"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Conversation not found", content = @Content),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "Conversation expired", content = @Content),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Invalid request", content = @Content),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Upstream relay failed", content = @Content),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Chatbot unavailable", content = @Content)
	})
	ResponseEntity<StreamingResponseBody> streamMessage(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "Chatbot streaming request",
			content = @Content(schema = @Schema(implementation = ChatbotMessageRequest.class))
		)
		ChatbotMessageRequest request
	);

	@Operation(summary = "Get chatbot conversation", description = "Relays chatbot conversation history lookup.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Latest conversation relayed"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No active conversation found", content = @Content),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "Conversation expired", content = @Content),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Upstream relay failed", content = @Content),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Chatbot unavailable", content = @Content)
	})
	ResponseEntity<String> getLatestConversation(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember
	);

	@Operation(summary = "Get chatbot conversation", description = "Relays chatbot conversation history lookup.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conversation relayed"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Conversation not found", content = @Content),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "Conversation expired", content = @Content),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Upstream relay failed", content = @Content),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Chatbot unavailable", content = @Content)
	})
	ResponseEntity<String> getConversation(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "Conversation id", required = true) String conversationId
	);
}

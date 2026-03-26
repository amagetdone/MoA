package com.ssafy.amagetdone.domain.chatbot.api.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.ssafy.amagetdone.domain.chatbot.api.dto.ChatbotMessageRequest;
import com.ssafy.amagetdone.domain.chatbot.application.service.ChatbotRelayService;
import com.ssafy.amagetdone.domain.chatbot.exception.ChatbotErrorCode;
import com.ssafy.amagetdone.domain.chatbot.exception.ChatbotRelayException;
import com.ssafy.amagetdone.domain.chatbot.infra.client.ChatbotRelayResult;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.global.config.SecurityConfig;
import com.ssafy.amagetdone.global.docs.controller.ChatbotDocsController;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(ChatbotController.class)
@Import(SecurityConfig.class)
class ChatbotControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtAccessTokenReader jwtAccessTokenReader;

	@MockitoBean
	private ChatbotRelayService chatbotRelayService;

	@Test
	void chatbotControllerImplementsDocsInterface() {
		assertThat(ChatbotDocsController.class).isAssignableFrom(ChatbotController.class);
	}

	@Test
	void postMessageDelegatesAuthenticatedUserAndReturnsRawJson() throws Exception {
		given(chatbotRelayService.relayMessage(
			eq(7),
			eq(new ChatbotMessageRequest("budget status?", "conv-1"))
		)).willReturn(ChatbotRelayResult.json(
			HttpStatus.OK,
			MediaType.APPLICATION_JSON,
			"""
			{"conversation_id":"conv-1","answer":"## Budget","warnings":[],"tool_summaries":[]}
			"""
		));

		mockMvc.perform(post("/api/v1/chatbot/messages")
				.contextPath("/api")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(7), null, List.of())
				))
				.contentType(APPLICATION_JSON)
				.content("""
					{"message":"budget status?","conversation_id":"conv-1"}
					"""))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
			.andExpect(content().json("""
				{"conversation_id":"conv-1","answer":"## Budget","warnings":[],"tool_summaries":[]}
				"""));
	}

	@Test
	void getConversationDelegatesAuthenticatedUserAndReturnsUpstreamGone() throws Exception {
		given(chatbotRelayService.relayConversation(7, "conv-expired"))
			.willReturn(ChatbotRelayResult.json(
				HttpStatus.GONE,
				MediaType.APPLICATION_JSON,
				"""
				{"detail":"conversation expired"}
				"""
			));

		mockMvc.perform(get("/api/v1/chatbot/conversations/conv-expired")
				.contextPath("/api")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(7), null, List.of())
				)))
			.andExpect(status().isGone())
			.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
			.andExpect(content().json("""
				{"detail":"conversation expired"}
				"""));
	}

	@Test
	void getLatestConversationDelegatesAuthenticatedUserAndReturnsUpstreamNotFound() throws Exception {
		given(chatbotRelayService.relayLatestConversation(7))
			.willReturn(ChatbotRelayResult.json(
				HttpStatus.NOT_FOUND,
				MediaType.APPLICATION_JSON,
				"""
				{"detail":"active conversation not found"}
				"""
			));

		mockMvc.perform(get("/api/v1/chatbot/conversations/latest")
				.contextPath("/api")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(7), null, List.of())
				)))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
			.andExpect(content().json("""
				{"detail":"active conversation not found"}
				"""));
	}

	@Test
	void postMessageWithoutAuthorizationReturnsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/v1/chatbot/messages")
				.contextPath("/api")
				.contentType(APPLICATION_JSON)
				.content("""
					{"message":"budget status?"}
					"""))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void upstreamRelayFailureReturnsBadGateway() throws Exception {
		given(chatbotRelayService.relayMessage(
			eq(7),
			eq(new ChatbotMessageRequest("budget status?", null))
		)).willThrow(new ChatbotRelayException(ChatbotErrorCode.CHATBOT_UPSTREAM_UNAVAILABLE));

		mockMvc.perform(post("/api/v1/chatbot/messages")
				.contextPath("/api")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(7), null, List.of())
				))
				.contentType(APPLICATION_JSON)
				.content("""
					{"message":"budget status?"}
					"""))
			.andExpect(status().isBadGateway())
			.andExpect(jsonPath("$.errorCode").value(ChatbotErrorCode.CHATBOT_UPSTREAM_UNAVAILABLE.code()));
	}
}

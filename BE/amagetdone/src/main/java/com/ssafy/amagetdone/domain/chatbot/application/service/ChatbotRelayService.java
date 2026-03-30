package com.ssafy.amagetdone.domain.chatbot.application.service;

import com.ssafy.amagetdone.domain.chatbot.api.dto.ChatbotMessageRequest;
import com.ssafy.amagetdone.domain.chatbot.infra.client.ChatbotApiClient;
import com.ssafy.amagetdone.domain.chatbot.infra.client.ChatbotRelayResult;

import org.springframework.stereotype.Service;

@Service
public class ChatbotRelayService {

	private final ChatbotApiClient chatbotApiClient;

	public ChatbotRelayService(ChatbotApiClient chatbotApiClient) {
		this.chatbotApiClient = chatbotApiClient;
	}

	public ChatbotRelayResult relayMessage(int userId, ChatbotMessageRequest request) {
		return chatbotApiClient.postMessage(userId, request);
	}

	public ChatbotRelayResult relayStream(int userId, ChatbotMessageRequest request) {
		return chatbotApiClient.streamMessage(userId, request);
	}

	public ChatbotRelayResult relayLatestConversation(int userId) {
		return chatbotApiClient.getLatestConversation(userId);
	}

	public ChatbotRelayResult relayConversation(int userId, String conversationId) {
		return chatbotApiClient.getConversation(userId, conversationId);
	}
}

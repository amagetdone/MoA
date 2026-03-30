package com.ssafy.amagetdone.domain.chatbot.api.dto;

public record ChatbotMessageRequest(
	String message,
	String conversation_id
) {
}

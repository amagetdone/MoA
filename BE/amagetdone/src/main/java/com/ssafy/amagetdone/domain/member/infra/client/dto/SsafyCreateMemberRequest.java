package com.ssafy.amagetdone.domain.member.infra.client.dto;

public record SsafyCreateMemberRequest(
	String apiKey,
	String userId
) {
}

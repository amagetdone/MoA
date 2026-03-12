package com.ssafy.amagetdone.domain.member.infra.client.dto;

public record SsafySearchMemberRequest(
	String userId,
	String apiKey
) {
}

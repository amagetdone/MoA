package com.ssafy.amagetdone.domain.member.infra.client.dto;

public record SsafyMemberResponse(
	String userId,
	String userName,
	String institutionCode,
	String userKey,
	String created,
	String modified
) {
}

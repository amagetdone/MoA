package com.ssafy.amagetdone.domain.member.support.util;

public record MemberTokenPair(
	String accessToken,
	String refreshToken
) {
}

package com.ssafy.amagetdone.domain.member.infra.client;

import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;

public record SocialUserIdentity(
	SocialProvider provider,
	String providerUid,
	String nickname,
	String email,
	String profileImageUrl
) {
	public SocialUserIdentity(SocialProvider provider, String providerUid) {
		this(provider, providerUid, null, null, null);
	}
}

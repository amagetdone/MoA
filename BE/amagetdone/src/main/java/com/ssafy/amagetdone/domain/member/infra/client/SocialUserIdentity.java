package com.ssafy.amagetdone.domain.member.infra.client;

import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;

public record SocialUserIdentity(
	SocialProvider provider,
	String providerUid
) {
}

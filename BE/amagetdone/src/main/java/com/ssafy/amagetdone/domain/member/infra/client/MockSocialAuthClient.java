package com.ssafy.amagetdone.domain.member.infra.client;

import com.ssafy.amagetdone.domain.member.api.dto.SocialAuthDto;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;

import org.springframework.stereotype.Component;

@Component
public class MockSocialAuthClient implements SocialAuthClient {

	public static final String TEST_PROVIDER_UID = "test-social-user-001";

	@Override
	public boolean supports(SocialProvider provider) {
		return provider == SocialProvider.TEST;
	}

	@Override
	public SocialUserIdentity fetchIdentity(SocialAuthDto.Request request) {
		return new SocialUserIdentity(SocialProvider.TEST, TEST_PROVIDER_UID);
	}
}

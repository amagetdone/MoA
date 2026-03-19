package com.ssafy.amagetdone.domain.member.infra.client;

import com.ssafy.amagetdone.domain.member.api.dto.SocialAuthDto;
import com.ssafy.amagetdone.domain.member.api.dto.TestSocialAuthDto;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;

import org.springframework.stereotype.Component;

@Component
public class MockSocialAuthClient implements SocialAuthClient {

	private static final String TEST_PROVIDER_UID_FORMAT = "test-social-user-%03d";
	public static final int DEFAULT_TEST_USER_ID = 1;
	public static final String TEST_PROVIDER_UID = providerUidFor(DEFAULT_TEST_USER_ID);

	@Override
	public boolean supports(SocialProvider provider) {
		return provider == SocialProvider.TEST;
	}

	@Override
	public SocialUserIdentity fetchIdentity(SocialAuthDto.Request request) {
		return new SocialUserIdentity(SocialProvider.TEST, providerUidFor(DEFAULT_TEST_USER_ID));
	}

	public SocialUserIdentity fetchIdentity(TestSocialAuthDto.Request request) {
		return new SocialUserIdentity(SocialProvider.TEST, providerUidFor(request.id()));
	}

	public static String providerUidFor(int testUserId) {
		return TEST_PROVIDER_UID_FORMAT.formatted(testUserId);
	}

	public static int resolveTestUserId(String providerUid) {
		int lastDashIndex = providerUid.lastIndexOf('-');
		if (lastDashIndex < 0 || lastDashIndex == providerUid.length() - 1) {
			return DEFAULT_TEST_USER_ID;
		}
		return Integer.parseInt(providerUid.substring(lastDashIndex + 1));
	}
}

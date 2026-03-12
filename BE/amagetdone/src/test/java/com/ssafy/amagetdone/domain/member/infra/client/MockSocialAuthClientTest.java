package com.ssafy.amagetdone.domain.member.infra.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ssafy.amagetdone.domain.member.api.dto.SocialAuthDto;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;

import org.junit.jupiter.api.Test;

class MockSocialAuthClientTest {

	@Test
	void supportsTestProviderAndReturnsFixedIdentity() {
		MockSocialAuthClient client = new MockSocialAuthClient();

		assertTrue(client.supports(SocialProvider.TEST));
		assertEquals(
			new SocialUserIdentity(SocialProvider.TEST, "test-social-user-001"),
			client.fetchIdentity(new SocialAuthDto.Request(null, null))
		);
	}
}

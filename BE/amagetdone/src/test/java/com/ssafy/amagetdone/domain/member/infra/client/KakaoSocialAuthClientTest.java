package com.ssafy.amagetdone.domain.member.infra.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ssafy.amagetdone.global.config.SocialAuthProperties;
import com.ssafy.amagetdone.domain.member.api.dto.SocialAuthDto;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;

class KakaoSocialAuthClientTest {

	@Test
	void fetchIdentityReadsProviderUidFromKakaoUserInfo() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		SocialAuthProperties properties = new SocialAuthProperties();
		properties.getKakao().setUserInfoUri("https://kapi.kakao.com/v2/user/me");
		KakaoSocialAuthClient client = new KakaoSocialAuthClient(builder, properties);

		server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
			.andExpect(method(GET))
			.andExpect(header(AUTHORIZATION, "Bearer kakao-token"))
			.andRespond(withSuccess("""
				{
				  "id": 987654321,
				  "kakao_account": {
				    "email": "moa@example.com",
				    "isEmailValid": true,
				    "isEmailVerified": true,
				    "profile": {
				      "nickname": "모아",
				      "profile_image_url": "https://k.kakaocdn.net/profile.png"
				    }
				  }
				}
				""", APPLICATION_JSON));

		SocialUserIdentity identity = client.fetchIdentity(new SocialAuthDto.Request("kakao-token", null));

		assertEquals(SocialProvider.KAKAO, identity.provider());
		assertEquals("987654321", identity.providerUid());
		assertEquals("모아", identity.nickname());
		assertEquals("moa@example.com", identity.email());
		assertEquals("https://k.kakaocdn.net/profile.png", identity.profileImageUrl());
	}
}

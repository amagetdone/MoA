package com.ssafy.amagetdone.domain.member.infra.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ssafy.amagetdone.config.SocialAuthProperties;
import com.ssafy.amagetdone.domain.member.api.dto.SocialAuthDto;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GoogleSocialAuthClientTest {

	@Test
	void fetchIdentityUsesIdTokenWhenProvided() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		SocialAuthProperties properties = new SocialAuthProperties();
		properties.getGoogle().setTokenInfoUri("https://oauth2.googleapis.com/tokeninfo");
		GoogleSocialAuthClient client = new GoogleSocialAuthClient(builder, properties);

		server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=google-id-token"))
			.andExpect(method(GET))
			.andRespond(withSuccess("""
				{
				  "sub": "google-user-123"
				}
				""", APPLICATION_JSON));

		SocialUserIdentity identity = client.fetchIdentity(new SocialAuthDto.Request(null, "google-id-token"));

		assertEquals(SocialProvider.GOOGLE, identity.provider());
		assertEquals("google-user-123", identity.providerUid());
	}

	@Test
	void fetchIdentityFallsBackToAccessTokenUserInfo() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		SocialAuthProperties properties = new SocialAuthProperties();
		properties.getGoogle().setUserInfoUri("https://openidconnect.googleapis.com/v1/userinfo");
		GoogleSocialAuthClient client = new GoogleSocialAuthClient(builder, properties);

		server.expect(requestTo("https://openidconnect.googleapis.com/v1/userinfo"))
			.andExpect(method(GET))
			.andExpect(header(AUTHORIZATION, "Bearer google-access-token"))
			.andRespond(withSuccess("""
				{
				  "sub": "google-user-456"
				}
				""", APPLICATION_JSON));

		SocialUserIdentity identity = client.fetchIdentity(new SocialAuthDto.Request("google-access-token", null));

		assertEquals(SocialProvider.GOOGLE, identity.provider());
		assertEquals("google-user-456", identity.providerUid());
	}
}

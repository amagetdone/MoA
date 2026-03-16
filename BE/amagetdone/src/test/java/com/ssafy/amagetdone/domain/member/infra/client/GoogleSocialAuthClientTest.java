package com.ssafy.amagetdone.domain.member.infra.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;

import com.ssafy.amagetdone.global.config.SocialAuthProperties;
import com.ssafy.amagetdone.domain.member.api.dto.SocialAuthDto;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GoogleSocialAuthClientTest {

	@Test
	void fetchIdentityUsesIdTokenWhenProvided() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		SocialAuthProperties properties = new SocialAuthProperties();
		properties.getGoogle().setClientId("expected-client-id");
		properties.getGoogle().setTokenInfoUri("https://oauth2.googleapis.com/tokeninfo");
		GoogleSocialAuthClient client = new GoogleSocialAuthClient(builder, properties);

		server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=google-id-token"))
			.andExpect(method(GET))
			.andRespond(withSuccess("""
				{
				  "aud": "expected-client-id",
				  "iss": "https://accounts.google.com",
				  "exp": "%s",
				  "sub": "google-user-123"
				}
				""".formatted(Instant.now().plusSeconds(300).getEpochSecond()), APPLICATION_JSON));

		SocialUserIdentity identity = client.fetchIdentity(new SocialAuthDto.Request(null, "google-id-token"));

		assertEquals(SocialProvider.GOOGLE, identity.provider());
		assertEquals("google-user-123", identity.providerUid());
	}

	@Test
	void fetchIdentityRejectsAccessTokenOnlyRequest() {
		RestClient.Builder builder = RestClient.builder();
		SocialAuthProperties properties = new SocialAuthProperties();
		GoogleSocialAuthClient client = new GoogleSocialAuthClient(builder, properties);

		MemberException exception = assertThrows(
			MemberException.class,
			() -> client.fetchIdentity(new SocialAuthDto.Request("google-access-token", null))
		);

		assertEquals("MEMBER_002", exception.getErrorCode().code());
	}

	@Test
	void fetchIdentityRejectsIdTokenWithUnexpectedAudience() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		SocialAuthProperties properties = new SocialAuthProperties();
		properties.getGoogle().setTokenInfoUri("https://oauth2.googleapis.com/tokeninfo");
		ReflectionTestUtils.setField(properties.getGoogle(), "clientId", "expected-client-id");
		GoogleSocialAuthClient client = new GoogleSocialAuthClient(builder, properties);

		server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=google-id-token"))
			.andExpect(method(GET))
			.andRespond(withSuccess("""
				{
				  "sub": "google-user-123",
				  "aud": "other-client-id"
				}
				""", APPLICATION_JSON));

		MemberException exception = assertThrows(
			MemberException.class,
			() -> client.fetchIdentity(new SocialAuthDto.Request(null, "google-id-token"))
		);

		assertEquals("MEMBER_003", exception.getErrorCode().code());
	}

	@Test
	void fetchIdentityRejectsIdTokenWithUnexpectedIssuer() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		SocialAuthProperties properties = new SocialAuthProperties();
		properties.getGoogle().setClientId("expected-client-id");
		properties.getGoogle().setTokenInfoUri("https://oauth2.googleapis.com/tokeninfo");
		GoogleSocialAuthClient client = new GoogleSocialAuthClient(builder, properties);

		server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=google-id-token"))
			.andExpect(method(GET))
			.andRespond(withSuccess("""
				{
				  "aud": "expected-client-id",
				  "iss": "https://malicious.example.com",
				  "exp": "%s",
				  "sub": "google-user-123"
				}
				""".formatted(Instant.now().plusSeconds(300).getEpochSecond()), APPLICATION_JSON));

		MemberException exception = assertThrows(
			MemberException.class,
			() -> client.fetchIdentity(new SocialAuthDto.Request(null, "google-id-token"))
		);

		assertEquals("MEMBER_003", exception.getErrorCode().code());
	}

	@Test
	void fetchIdentityRejectsExpiredIdToken() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		SocialAuthProperties properties = new SocialAuthProperties();
		properties.getGoogle().setClientId("expected-client-id");
		properties.getGoogle().setTokenInfoUri("https://oauth2.googleapis.com/tokeninfo");
		GoogleSocialAuthClient client = new GoogleSocialAuthClient(builder, properties);

		server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=google-id-token"))
			.andExpect(method(GET))
			.andRespond(withSuccess("""
				{
				  "aud": "expected-client-id",
				  "iss": "https://accounts.google.com",
				  "exp": "%s",
				  "sub": "google-user-123"
				}
				""".formatted(Instant.now().minusSeconds(60).getEpochSecond()), APPLICATION_JSON));

		MemberException exception = assertThrows(
			MemberException.class,
			() -> client.fetchIdentity(new SocialAuthDto.Request(null, "google-id-token"))
		);

		assertEquals("MEMBER_003", exception.getErrorCode().code());
	}
}

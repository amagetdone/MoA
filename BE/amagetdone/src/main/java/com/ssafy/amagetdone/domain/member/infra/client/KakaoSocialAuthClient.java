package com.ssafy.amagetdone.domain.member.infra.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.amagetdone.global.config.SocialAuthProperties;
import com.ssafy.amagetdone.domain.member.api.dto.SocialAuthDto;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoSocialAuthClient implements SocialAuthClient {

	private final RestClient restClient;
	private final SocialAuthProperties properties;

	public KakaoSocialAuthClient(RestClient.Builder restClientBuilder, SocialAuthProperties properties) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
	}

	@Override
	public boolean supports(SocialProvider provider) {
		return provider == SocialProvider.KAKAO;
	}

	@Override
	public SocialUserIdentity fetchIdentity(SocialAuthDto.Request request) {
		if (request.accessToken() == null || request.accessToken().isBlank()) {
			throw new MemberException(MemberErrorCode.KAKAO_ACCESS_TOKEN_REQUIRED);
		}

		try {
			KakaoUserResponse response = restClient.get()
				.uri(properties.getKakao().getUserInfoUri())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + request.accessToken())
				.retrieve()
				.body(KakaoUserResponse.class);

			if (response == null || response.id() == null) {
				throw new MemberException(MemberErrorCode.INVALID_KAKAO_ACCESS_TOKEN);
			}

			KakaoAccount kakaoAccount = response.kakaoAccount();
			Profile profile = kakaoAccount != null ? kakaoAccount.profile() : null;

			return new SocialUserIdentity(
				SocialProvider.KAKAO,
				String.valueOf(response.id()),
				profile != null ? profile.nickname() : null,
				resolveEmail(kakaoAccount),
				profile != null ? profile.profileImageUrl() : null
			);
		} catch (RestClientException exception) {
			throw new MemberException(MemberErrorCode.INVALID_KAKAO_ACCESS_TOKEN, exception);
		}
	}

	private String resolveEmail(KakaoAccount kakaoAccount) {
		if (kakaoAccount == null || kakaoAccount.email() == null || kakaoAccount.email().isBlank()) {
			return null;
		}
		if (Boolean.FALSE.equals(kakaoAccount.isEmailValid()) || Boolean.FALSE.equals(kakaoAccount.isEmailVerified())) {
			return null;
		}
		return kakaoAccount.email();
	}

	private record KakaoUserResponse(
		Long id,
		@JsonProperty("kakao_account")
		KakaoAccount kakaoAccount
	) {
	}

	private record KakaoAccount(
		String email,
		@JsonProperty("is_email_valid")
		Boolean isEmailValid,
		@JsonProperty("is_email_verified")
		Boolean isEmailVerified,
		Profile profile
	) {
	}

	private record Profile(
		String nickname,
		@JsonProperty("profile_image_url")
		String profileImageUrl
	) {
	}
}

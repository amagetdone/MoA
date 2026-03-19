package com.ssafy.amagetdone.domain.member.infra.client;

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

			return new SocialUserIdentity(SocialProvider.KAKAO, String.valueOf(response.id()));
		} catch (RestClientException exception) {
			throw new MemberException(MemberErrorCode.INVALID_KAKAO_ACCESS_TOKEN, exception);
		}
	}

	private record KakaoUserResponse(
		Long id
	) {
	}
}

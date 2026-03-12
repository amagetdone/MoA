package com.ssafy.amagetdone.domain.member.infra.client;

import java.nio.charset.StandardCharsets;

import com.ssafy.amagetdone.config.SocialAuthProperties;
import com.ssafy.amagetdone.domain.member.api.dto.SocialAuthDto;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GoogleSocialAuthClient implements SocialAuthClient {

	private final RestClient restClient;
	private final SocialAuthProperties properties;

	public GoogleSocialAuthClient(RestClient.Builder restClientBuilder, SocialAuthProperties properties) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
	}

	@Override
	public boolean supports(SocialProvider provider) {
		return provider == SocialProvider.GOOGLE;
	}

	@Override
	public SocialUserIdentity fetchIdentity(SocialAuthDto.Request request) {
		if (request.idToken() != null && !request.idToken().isBlank()) {
			return fetchByIdToken(request.idToken());
		}

		throw new MemberException(MemberErrorCode.GOOGLE_ID_TOKEN_REQUIRED);
	}

	private SocialUserIdentity fetchByIdToken(String idToken) {
		String uri = UriComponentsBuilder.fromUriString(properties.getGoogle().getTokenInfoUri())
			.queryParam("id_token", idToken)
			.encode(StandardCharsets.UTF_8)
			.toUriString();

		try {
			GoogleTokenInfoResponse response = restClient.get()
				.uri(uri)
				.retrieve()
				.body(GoogleTokenInfoResponse.class);

			if (response == null || response.sub() == null || response.sub().isBlank()) {
				throw new MemberException(MemberErrorCode.INVALID_GOOGLE_ID_TOKEN);
			}

			return new SocialUserIdentity(SocialProvider.GOOGLE, response.sub());
		} catch (RestClientException exception) {
			throw new MemberException(MemberErrorCode.INVALID_GOOGLE_ID_TOKEN, exception);
		}
	}

	private record GoogleTokenInfoResponse(
		String sub
	) {
	}
}

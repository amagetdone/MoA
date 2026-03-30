package com.ssafy.amagetdone.domain.member.infra.client;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;

import com.ssafy.amagetdone.global.config.SocialAuthProperties;
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

	private static final Set<String> VALID_GOOGLE_ISSUERS = Set.of(
		"accounts.google.com",
		"https://accounts.google.com"
	);

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
		SocialUserIdentity idTokenIdentity = null;
		if (request.idToken() != null && !request.idToken().isBlank()) {
			try {
				idTokenIdentity = fetchByIdToken(request.idToken());
			} catch (MemberException exception) {
				if (request.accessToken() == null || request.accessToken().isBlank()) {
					throw exception;
				}
			}
		}

		if (request.accessToken() != null && !request.accessToken().isBlank()) {
			SocialUserIdentity accessTokenIdentity = fetchByAccessToken(request.accessToken());
			if (idTokenIdentity == null) {
				return accessTokenIdentity;
			}
			if (!idTokenIdentity.providerUid().equals(accessTokenIdentity.providerUid())) {
				throw new MemberException(MemberErrorCode.INVALID_GOOGLE_ID_TOKEN);
			}
			return mergeIdentity(idTokenIdentity, accessTokenIdentity);
		}

		if (idTokenIdentity != null) {
			return idTokenIdentity;
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

			if (!isValidGoogleTokenInfo(response)) {
				throw new MemberException(MemberErrorCode.INVALID_GOOGLE_ID_TOKEN);
			}

			return new SocialUserIdentity(
				SocialProvider.GOOGLE,
				response.sub(),
				null,
				normalizeEmail(response.email()),
				null
			);
		} catch (RestClientException exception) {
			throw new MemberException(MemberErrorCode.INVALID_GOOGLE_ID_TOKEN, exception);
		}
	}

	private SocialUserIdentity fetchByAccessToken(String accessToken) {
		String uri = UriComponentsBuilder.fromUriString(properties.getGoogle().getTokenInfoUri())
			.queryParam("access_token", accessToken)
			.encode(StandardCharsets.UTF_8)
			.toUriString();

		try {
			GoogleTokenInfoResponse response = restClient.get()
				.uri(uri)
				.retrieve()
				.body(GoogleTokenInfoResponse.class);

			if (!isValidGoogleAccessTokenInfo(response)) {
				throw new MemberException(MemberErrorCode.INVALID_GOOGLE_ID_TOKEN);
			}

			return mergeIdentity(
				new SocialUserIdentity(
					SocialProvider.GOOGLE,
					response.sub(),
					null,
					normalizeEmail(response.email()),
					null
				),
				fetchGoogleUserInfo(accessToken)
			);
		} catch (RestClientException exception) {
			throw new MemberException(MemberErrorCode.INVALID_GOOGLE_ID_TOKEN, exception);
		}
	}

	private SocialUserIdentity fetchGoogleUserInfo(String accessToken) {
		if (properties.getGoogle().getUserInfoUri() == null || properties.getGoogle().getUserInfoUri().isBlank()) {
			return new SocialUserIdentity(SocialProvider.GOOGLE, "");
		}

		try {
			GoogleUserInfoResponse response = restClient.get()
				.uri(properties.getGoogle().getUserInfoUri())
				.headers(headers -> headers.setBearerAuth(accessToken))
				.retrieve()
				.body(GoogleUserInfoResponse.class);

			if (response == null || response.sub() == null || response.sub().isBlank()) {
				return new SocialUserIdentity(SocialProvider.GOOGLE, "");
			}

			return new SocialUserIdentity(
				SocialProvider.GOOGLE,
				response.sub(),
				response.name(),
				normalizeEmail(response.email()),
				response.picture()
			);
		} catch (RestClientException exception) {
			return new SocialUserIdentity(SocialProvider.GOOGLE, "");
		}
	}

	private SocialUserIdentity mergeIdentity(SocialUserIdentity primary, SocialUserIdentity secondary) {
		return new SocialUserIdentity(
			primary.provider(),
			primary.providerUid(),
			firstNonBlank(primary.nickname(), secondary.nickname()),
			firstNonBlank(primary.email(), secondary.email()),
			firstNonBlank(primary.profileImageUrl(), secondary.profileImageUrl())
		);
	}

	private String firstNonBlank(String primary, String secondary) {
		if (primary != null && !primary.isBlank()) {
			return primary;
		}
		if (secondary != null && !secondary.isBlank()) {
			return secondary;
		}
		return null;
	}

	private String normalizeEmail(String email) {
		return email != null && !email.isBlank() ? email : null;
	}

	private boolean isValidGoogleTokenInfo(GoogleTokenInfoResponse response) {
		return response != null
			&& response.sub() != null
			&& !response.sub().isBlank()
			&& properties.getGoogle().getClientId() != null
			&& !properties.getGoogle().getClientId().isBlank()
			&& properties.getGoogle().getClientId().equals(response.aud())
			&& VALID_GOOGLE_ISSUERS.contains(response.iss())
			&& isNotExpired(response.exp());
	}

	private boolean isValidGoogleAccessTokenInfo(GoogleTokenInfoResponse response) {
		return response != null
			&& response.sub() != null
			&& !response.sub().isBlank()
			&& properties.getGoogle().getClientId() != null
			&& !properties.getGoogle().getClientId().isBlank()
			&& properties.getGoogle().getClientId().equals(response.aud())
			&& isNotExpired(response.exp());
	}

	private boolean isNotExpired(String expirationEpochSeconds) {
		try {
			return Long.parseLong(expirationEpochSeconds) > Instant.now().getEpochSecond();
		} catch (RuntimeException exception) {
			return false;
		}
	}

	private record GoogleTokenInfoResponse(
		String aud,
		String exp,
		String iss,
		String sub,
		String email
	) {
	}

	private record GoogleUserInfoResponse(
		String sub,
		String email,
		String name,
		String picture
	) {
	}
}

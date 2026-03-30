package com.ssafy.amagetdone.domain.member.infra.client;

import com.ssafy.amagetdone.global.config.SsafyFinApiProperties;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.infra.client.dto.SsafyCreateMemberRequest;
import com.ssafy.amagetdone.domain.member.infra.client.dto.SsafyMemberResponse;
import com.ssafy.amagetdone.domain.member.infra.client.dto.SsafySearchMemberRequest;
import com.ssafy.amagetdone.domain.member.support.log.MemberLogHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class SsafyFinMemberClient {

	private static final Logger log = LoggerFactory.getLogger(SsafyFinMemberClient.class);
	private static final String DUPLICATE_ID_CODE = "E4002";
	private static final String NOT_FOUND_ID_CODE = "E4003";

	private final RestClient restClient;
	private final SsafyFinApiProperties properties;

	public SsafyFinMemberClient(RestClient.Builder restClientBuilder, SsafyFinApiProperties properties) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
	}

	public SsafyMemberResponse createMember(String email) {
		requireApiKey();
		String requestId = MemberLogHelper.requestId();
		String maskedEmail = MemberLogHelper.maskEmail(email);
		log.info("event=FIN_MEMBER_CREATE_ATTEMPT requestId={} userIdMasked={}", requestId, maskedEmail);
		try {
			SsafyMemberResponse response = restClient.post()
				.uri(joinUrl("/member"))
				.contentType(MediaType.APPLICATION_JSON)
				.body(new SsafyCreateMemberRequest(properties.getApiKey(), email))
				.retrieve()
				.body(SsafyMemberResponse.class);

			SsafyMemberResponse validated = validateResponse(response, MemberErrorCode.FINANCIAL_MEMBER_CREATE_FAILED);
			log.info("event=FIN_MEMBER_CREATE_SUCCESS requestId={} userIdMasked={}", requestId, maskedEmail);
			return validated;
		} catch (RestClientResponseException exception) {
			MemberException translated = translateException(exception, MemberErrorCode.FINANCIAL_MEMBER_CREATE_FAILED);
			log.warn(
				"event=FIN_MEMBER_CREATE_FAILURE requestId={} userIdMasked={} errorCode={}",
				requestId,
				maskedEmail,
				translated.getErrorCode().code()
			);
			throw translated;
		} catch (RestClientException exception) {
			log.warn(
				"event=FIN_MEMBER_CREATE_FAILURE requestId={} userIdMasked={} errorCode={}",
				requestId,
				maskedEmail,
				MemberErrorCode.FINANCIAL_MEMBER_CREATE_FAILED.code()
			);
			throw new MemberException(MemberErrorCode.FINANCIAL_MEMBER_CREATE_FAILED, exception);
		}
	}

	public SsafyMemberResponse searchMember(String email) {
		requireApiKey();
		String requestId = MemberLogHelper.requestId();
		String maskedEmail = MemberLogHelper.maskEmail(email);
		log.info("event=FIN_MEMBER_SEARCH_ATTEMPT requestId={} userIdMasked={}", requestId, maskedEmail);
		try {
			SsafyMemberResponse response = restClient.post()
				.uri(joinUrl("/member/search"))
				.contentType(MediaType.APPLICATION_JSON)
				.body(new SsafySearchMemberRequest(email, properties.getApiKey()))
				.retrieve()
				.body(SsafyMemberResponse.class);

			SsafyMemberResponse validated = validateResponse(response, MemberErrorCode.FINANCIAL_MEMBER_SEARCH_FAILED);
			log.info("event=FIN_MEMBER_SEARCH_SUCCESS requestId={} userIdMasked={}", requestId, maskedEmail);
			return validated;
		} catch (RestClientResponseException exception) {
			MemberException translated = translateException(exception, MemberErrorCode.FINANCIAL_MEMBER_SEARCH_FAILED);
			log.warn(
				"event=FIN_MEMBER_SEARCH_FAILURE requestId={} userIdMasked={} errorCode={}",
				requestId,
				maskedEmail,
				translated.getErrorCode().code()
			);
			throw translated;
		} catch (RestClientException exception) {
			log.warn(
				"event=FIN_MEMBER_SEARCH_FAILURE requestId={} userIdMasked={} errorCode={}",
				requestId,
				maskedEmail,
				MemberErrorCode.FINANCIAL_MEMBER_SEARCH_FAILED.code()
			);
			throw new MemberException(MemberErrorCode.FINANCIAL_MEMBER_SEARCH_FAILED, exception);
		}
	}

	private void requireApiKey() {
		if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
			throw new MemberException(MemberErrorCode.FINANCIAL_API_KEY_MISSING);
		}
	}

	private String joinUrl(String path) {
		String baseUrl = properties.getBaseUrl();
		if (baseUrl == null || baseUrl.isBlank()) {
			throw new MemberException(MemberErrorCode.FINANCIAL_MEMBER_LINK_FAILED);
		}
		if (baseUrl.endsWith("/")) {
			return baseUrl.substring(0, baseUrl.length() - 1) + path;
		}
		return baseUrl + path;
	}

	private SsafyMemberResponse validateResponse(SsafyMemberResponse response, MemberErrorCode errorCode) {
		if (response == null || response.userKey() == null || response.userKey().isBlank()) {
			throw new MemberException(errorCode);
		}
		return response;
	}

	private MemberException translateException(RestClientResponseException exception, MemberErrorCode fallbackCode) {
		String body = exception.getResponseBodyAsString();
		if (containsCode(body, DUPLICATE_ID_CODE)) {
			return new MemberException(MemberErrorCode.FINANCIAL_MEMBER_ALREADY_EXISTS, exception);
		}
		if (containsCode(body, NOT_FOUND_ID_CODE)) {
			return new MemberException(MemberErrorCode.FINANCIAL_MEMBER_NOT_FOUND, exception);
		}
		return new MemberException(fallbackCode, exception);
	}

	private boolean containsCode(String body, String code) {
		return body != null && body.contains(code);
	}
}

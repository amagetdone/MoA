package com.ssafy.amagetdone.domain.accountverification.infra.client;

import com.ssafy.amagetdone.domain.accountverification.exception.AccountVerificationErrorCode;
import com.ssafy.amagetdone.domain.accountverification.exception.AccountVerificationException;
import com.ssafy.amagetdone.domain.accountverification.infra.client.dto.AccountVerificationClientDto;
import com.ssafy.amagetdone.global.infra.ssafy.envelope.SsafyResponseEnvelope;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiRequestFactory;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiResponseValidator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class SsafyAccountVerificationApiClient {

	private static final String REQUEST_VERIFICATION_API = "openAccountAuth";
	private static final String CHECK_VERIFICATION_API = "checkAuthCode";
	private static final String SUCCESS_CODE = "H0000";

	private final RestClient restClient;
	private final AccountVerificationSsafyApiProperties properties;
	private final SsafyApiRequestFactory requestFactory;
	private final SsafyApiResponseValidator responseValidator;
	private final ParameterizedTypeReference<SsafyResponseEnvelope<RequestVerificationApiResponse>> requestVerificationType;
	private final ParameterizedTypeReference<SsafyResponseEnvelope<CheckVerificationApiResponse>> checkVerificationType;

	@Value("${account.ssafy.api-key}")
	private String apiKey;

	public SsafyAccountVerificationApiClient(
		RestClient.Builder restClientBuilder,
		AccountVerificationSsafyApiProperties properties,
		SsafyApiRequestFactory requestFactory,
		SsafyApiResponseValidator responseValidator
	) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
		this.requestFactory = requestFactory;
		this.responseValidator = responseValidator;
		this.requestVerificationType = new ParameterizedTypeReference<>() {
		};
		this.checkVerificationType = new ParameterizedTypeReference<>() {
		};
	}

	public AccountVerificationClientDto.RequestVerificationResponse requestVerification(
		AccountVerificationClientDto.RequestVerificationRequest request
	) {
		validateConfiguration();
		validateRequest(request == null ? null : request.userKey(), request == null ? null : request.accountNo());
		RequestVerificationApiResponse payload = post(
			REQUEST_VERIFICATION_API,
			request.userKey(),
			new RequestVerificationApiRequest(request.accountNo(), request.authText()),
			requestVerificationType
		);
		if (payload.transactionUniqueNo() == null || isBlank(payload.accountNo())) {
			throw new AccountVerificationException(AccountVerificationErrorCode.INVALID_VERIFICATION_API_RESPONSE);
		}
		return new AccountVerificationClientDto.RequestVerificationResponse(
			payload.transactionUniqueNo(),
			payload.accountNo()
		);
	}

	public AccountVerificationClientDto.CheckVerificationResponse checkVerification(
		AccountVerificationClientDto.CheckVerificationRequest request
	) {
		validateConfiguration();
		validateRequest(request == null ? null : request.userKey(), request == null ? null : request.accountNo());
		if (request == null || isBlank(request.authText()) || isBlank(request.authCode())) {
			throw new AccountVerificationException(AccountVerificationErrorCode.VERIFICATION_API_CALL_FAILED);
		}
		CheckVerificationApiResponse payload = post(
			CHECK_VERIFICATION_API,
			request.userKey(),
			new CheckVerificationApiRequest(request.accountNo(), request.authText(), request.authCode()),
			checkVerificationType
		);
		if (isBlank(payload.status()) || payload.transactionUniqueNo() == null || isBlank(payload.accountNo())) {
			throw new AccountVerificationException(AccountVerificationErrorCode.INVALID_VERIFICATION_API_RESPONSE);
		}
		return new AccountVerificationClientDto.CheckVerificationResponse(
			payload.status(),
			payload.transactionUniqueNo(),
			payload.accountNo()
		);
	}

	private <T, R> R post(
		String apiName,
		String userKey,
		T body,
		ParameterizedTypeReference<SsafyResponseEnvelope<R>> responseType
	) {
		try {
			SsafyResponseEnvelope<R> envelope = restClient.post()
				.uri(properties.getBaseUrl() + "/accountAuth/" + apiName)
				.contentType(MediaType.APPLICATION_JSON)
				.body(requestFactory.create(
					apiName,
					properties.getInstitutionCode(),
					properties.getFintechAppNo(),
					apiKey,
					userKey,
					body
				))
				.retrieve()
				.body(responseType);

			if (envelope == null || envelope.header() == null) {
				throw new AccountVerificationException(AccountVerificationErrorCode.INVALID_VERIFICATION_API_RESPONSE);
			}
			if (!SUCCESS_CODE.equals(envelope.header().responseCode())) {
				throw new AccountVerificationException(resolveErrorCode(envelope.header().responseCode()));
			}

			R payload = responseValidator.validate(envelope).payload();
			if (payload == null) {
				throw new AccountVerificationException(AccountVerificationErrorCode.INVALID_VERIFICATION_API_RESPONSE);
			}
			return payload;
		} catch (AccountVerificationException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new AccountVerificationException(AccountVerificationErrorCode.VERIFICATION_API_CALL_FAILED, exception);
		}
	}

	private AccountVerificationErrorCode resolveErrorCode(String responseCode) {
		if ("A1086".equals(responseCode)) {
			return AccountVerificationErrorCode.AUTH_HISTORY_NOT_FOUND;
		}
		if ("A1087".equals(responseCode)) {
			return AccountVerificationErrorCode.AUTH_CODE_EXPIRED;
		}
		if ("A1088".equals(responseCode)) {
			return AccountVerificationErrorCode.AUTH_CODE_MISMATCH;
		}
		if ("A1089".equals(responseCode)) {
			return AccountVerificationErrorCode.INVALID_AUTH_TEXT;
		}
		if ("A1090".equals(responseCode)) {
			return AccountVerificationErrorCode.INVALID_AUTH_CODE;
		}
		return AccountVerificationErrorCode.INVALID_VERIFICATION_API_RESPONSE;
	}

	private void validateConfiguration() {
		if (isBlank(properties.getBaseUrl())
			|| isBlank(properties.getInstitutionCode())
			|| isBlank(properties.getFintechAppNo())
			|| isBlank(apiKey)) {
			throw new AccountVerificationException(AccountVerificationErrorCode.VERIFICATION_API_CALL_FAILED);
		}
	}

	private void validateRequest(String userKey, String accountNo) {
		if (isBlank(userKey) || isBlank(accountNo)) {
			throw new AccountVerificationException(AccountVerificationErrorCode.VERIFICATION_API_CALL_FAILED);
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private record RequestVerificationApiRequest(
		String accountNo,
		String authText
	) {
	}

	private record RequestVerificationApiResponse(
		Long transactionUniqueNo,
		String accountNo
	) {
	}

	private record CheckVerificationApiRequest(
		String accountNo,
		String authText,
		String authCode
	) {
	}

	private record CheckVerificationApiResponse(
		String status,
		Long transactionUniqueNo,
		String accountNo
	) {
	}
}

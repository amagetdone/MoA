package com.ssafy.amagetdone.domain.account.infra.client;

import java.util.List;

import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositProductDto;
import com.ssafy.amagetdone.global.infra.ssafy.envelope.SsafyResponseEnvelope;
import com.ssafy.amagetdone.global.infra.ssafy.exception.SsafyApiException;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiRequestFactory;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiResponseValidator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class SsafyDemandDepositProductApiClient {

	private static final String CREATE_API = "createDemandDeposit";
	private static final String LIST_API = "inquireDemandDepositList";

	private final RestClient restClient;
	private final AccountSsafyApiProperties properties;
	private final SsafyApiRequestFactory requestFactory;
	private final SsafyApiResponseValidator responseValidator;
	private final ParameterizedTypeReference<SsafyResponseEnvelope<DemandDepositProductDto.ApiProductResponse>> createResponseType;
	private final ParameterizedTypeReference<SsafyResponseEnvelope<List<DemandDepositProductDto.ApiProductResponse>>> listResponseType;

	@Value("${account.ssafy.api-key}")
	private String apiKey;

	public SsafyDemandDepositProductApiClient(
		RestClient.Builder restClientBuilder,
		AccountSsafyApiProperties properties,
		SsafyApiRequestFactory requestFactory,
		SsafyApiResponseValidator responseValidator
	) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
		this.requestFactory = requestFactory;
		this.responseValidator = responseValidator;
		this.createResponseType = new ParameterizedTypeReference<>() {
		};
		this.listResponseType = new ParameterizedTypeReference<>() {
		};
	}

	public DemandDepositProductDto.ProductResponse createProduct(DemandDepositProductDto.ProductCreateRequest request) {
		validateConfiguration();
		if (request == null || isBlank(request.bankCode()) || isBlank(request.accountName())) {
			throw new AccountException(AccountErrorCode.DEMAND_DEPOSIT_PRODUCT_API_CALL_FAILED);
		}

		DemandDepositProductDto.ApiProductResponse payload = post(
			CREATE_API,
			null,
			request,
			createResponseType,
			AccountErrorCode.DEMAND_DEPOSIT_PRODUCT_API_CALL_FAILED,
			AccountErrorCode.INVALID_DEMAND_DEPOSIT_PRODUCT_API_RESPONSE
		);

		return toProductResponse(payload);
	}

	public List<DemandDepositProductDto.ProductResponse> inquireProducts() {
		validateConfiguration();

		List<DemandDepositProductDto.ApiProductResponse> payload = post(
			LIST_API,
			null,
			new EmptyRequest(),
			listResponseType,
			AccountErrorCode.DEMAND_DEPOSIT_PRODUCT_API_CALL_FAILED,
			AccountErrorCode.INVALID_DEMAND_DEPOSIT_PRODUCT_API_RESPONSE
		);

		if (payload == null) {
			throw new AccountException(AccountErrorCode.INVALID_DEMAND_DEPOSIT_PRODUCT_API_RESPONSE);
		}

		return payload.stream()
			.map(this::toProductResponse)
			.toList();
	}

	private <T, R> R post(
		String apiName,
		String userKey,
		T body,
		ParameterizedTypeReference<SsafyResponseEnvelope<R>> responseType,
		AccountErrorCode callErrorCode,
		AccountErrorCode invalidResponseCode
	) {
		try {
			R payload = responseValidator.validate(
				restClient.post()
					.uri(properties.getBaseUrl() + "/demandDeposit/" + apiName)
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
					.body(responseType)
			).payload();

			if (payload == null) {
				throw new AccountException(invalidResponseCode);
			}
			return payload;
		} catch (SsafyApiException exception) {
			throw new AccountException(resolveSsafyError(exception, callErrorCode, invalidResponseCode), exception);
		} catch (RestClientException exception) {
			throw new AccountException(callErrorCode, exception);
		}
	}

	private DemandDepositProductDto.ProductResponse toProductResponse(DemandDepositProductDto.ApiProductResponse payload) {
		if (payload == null) {
			throw new AccountException(AccountErrorCode.INVALID_DEMAND_DEPOSIT_PRODUCT_API_RESPONSE);
		}
		return new DemandDepositProductDto.ProductResponse(
			payload.accountTypeUniqueNo(),
			payload.bankCode(),
			payload.bankName(),
			payload.accountTypeCode(),
			payload.accountTypeName(),
			payload.accountName(),
			payload.accountDescription(),
			payload.accountType()
		);
	}

	private AccountErrorCode resolveSsafyError(
		SsafyApiException exception,
		AccountErrorCode callErrorCode,
		AccountErrorCode invalidResponseCode
	) {
		if (exception.reason() == SsafyApiException.Reason.INVALID_CONFIGURATION
			|| exception.reason() == SsafyApiException.Reason.CALL_FAILED) {
			return callErrorCode;
		}
		return invalidResponseCode;
	}

	private void validateConfiguration() {
		if (isBlank(properties.getBaseUrl())
			|| isBlank(properties.getInstitutionCode())
			|| isBlank(properties.getFintechAppNo())
			|| isBlank(apiKey)) {
			throw new AccountException(AccountErrorCode.DEMAND_DEPOSIT_PRODUCT_API_CALL_FAILED);
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private record EmptyRequest() {
	}
}

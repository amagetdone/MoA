package com.ssafy.amagetdone.domain.transaction.infra.client;

import com.ssafy.amagetdone.domain.transaction.exception.TransactionErrorCode;
import com.ssafy.amagetdone.domain.transaction.exception.TransactionException;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.DemandDepositTransactionDto;
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
public class SsafyDemandDepositTransactionApiClient {

	private static final String API_NAME = "inquireTransactionHistoryList";

	private final RestClient restClient;
	private final TransactionSsafyApiProperties properties;
	private final SsafyApiRequestFactory requestFactory;
	private final SsafyApiResponseValidator responseValidator;
	private final ParameterizedTypeReference<SsafyResponseEnvelope<DemandDepositTransactionDto.ApiResponse>> responseType;

	@Value("${transaction.ssafy.api-key}")
	private String apiKey;

	public SsafyDemandDepositTransactionApiClient(
		RestClient.Builder restClientBuilder,
		TransactionSsafyApiProperties properties,
		SsafyApiRequestFactory requestFactory,
		SsafyApiResponseValidator responseValidator
	) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
		this.requestFactory = requestFactory;
		this.responseValidator = responseValidator;
		this.responseType = new ParameterizedTypeReference<>() {
		};
	}

	public DemandDepositTransactionDto.Response inquireTransactions(DemandDepositTransactionDto.Request request) {
		validateConfiguration();
		validateRequest(request);

		DemandDepositTransactionDto.ApiResponse record;
		try {
			record = responseValidator.validate(
				restClient.post()
				.uri(properties.getBaseUrl() + "/demandDeposit/" + API_NAME)
				.contentType(MediaType.APPLICATION_JSON)
				.body(requestFactory.create(
					API_NAME,
					properties.getInstitutionCode(),
					properties.getFintechAppNo(),
					apiKey,
					request.userKey(),
					new DemandDepositTransactionDto.ApiRequest(
						request.accountNo(),
						request.startDate(),
						request.endDate(),
						request.transactionType(),
						request.orderByType()
					)
				))
				.retrieve()
				.body(responseType)
			).payload();
		} catch (SsafyApiException exception) {
			throw new TransactionException(resolveErrorCode(exception), exception);
		} catch (RestClientException exception) {
			throw new TransactionException(TransactionErrorCode.DEMAND_DEPOSIT_TRANSACTION_API_CALL_FAILED, exception);
		}

		if (record == null || record.list() == null) {
			throw new TransactionException(TransactionErrorCode.INVALID_DEMAND_DEPOSIT_API_RESPONSE);
		}

		return new DemandDepositTransactionDto.Response(
			record.totalCount(),
			record.list().stream()
				.map(item -> new DemandDepositTransactionDto.Item(
					item.transactionUniqueNo(),
					item.transactionDate(),
					item.transactionTime(),
					item.transactionType(),
					item.transactionTypeName(),
					item.transactionAccountNo(),
					item.transactionBalance(),
					item.transactionAfterBalance(),
					item.transactionSummary(),
					item.transactionMemo()
				))
				.toList()
		);
	}

	private void validateConfiguration() {
		if (isBlank(properties.getBaseUrl())
			|| isBlank(properties.getInstitutionCode())
			|| isBlank(properties.getFintechAppNo())
			|| isBlank(apiKey)) {
			throw new TransactionException(TransactionErrorCode.DEMAND_DEPOSIT_TRANSACTION_API_CALL_FAILED);
		}
	}

	private void validateRequest(DemandDepositTransactionDto.Request request) {
		if (request == null
			|| isBlank(request.userKey())
			|| isBlank(request.accountNo())
			|| isBlank(request.startDate())
			|| isBlank(request.endDate())
			|| isBlank(request.transactionType())
			|| isBlank(request.orderByType())) {
			throw new TransactionException(TransactionErrorCode.DEMAND_DEPOSIT_TRANSACTION_API_CALL_FAILED);
		}
	}

	private TransactionErrorCode resolveErrorCode(SsafyApiException exception) {
		if (exception.reason() == SsafyApiException.Reason.INVALID_CONFIGURATION
			|| exception.reason() == SsafyApiException.Reason.CALL_FAILED) {
			return TransactionErrorCode.DEMAND_DEPOSIT_TRANSACTION_API_CALL_FAILED;
		}
		return TransactionErrorCode.INVALID_DEMAND_DEPOSIT_API_RESPONSE;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}

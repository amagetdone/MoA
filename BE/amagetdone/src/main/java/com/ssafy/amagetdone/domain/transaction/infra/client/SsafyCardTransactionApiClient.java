package com.ssafy.amagetdone.domain.transaction.infra.client;

import java.util.List;

import com.ssafy.amagetdone.domain.transaction.exception.TransactionErrorCode;
import com.ssafy.amagetdone.domain.transaction.exception.TransactionException;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.CardTransactionDto;
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
public class SsafyCardTransactionApiClient {

	private static final String API_NAME = "inquireCreditCardTransactionList";

	private final RestClient restClient;
	private final TransactionSsafyApiProperties properties;
	private final SsafyApiRequestFactory requestFactory;
	private final SsafyApiResponseValidator responseValidator;
	private final ParameterizedTypeReference<SsafyResponseEnvelope<CardTransactionDto.ApiResponse>> responseType;

	@Value("${transaction.ssafy.api-key}")
	private String apiKey;

	public SsafyCardTransactionApiClient(
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

	public CardTransactionDto.Response inquireTransactions(CardTransactionDto.Request request) {
		validateConfiguration();
		validateRequest(request);

		CardTransactionDto.ApiResponse record;
		try {
			record = responseValidator.validate(
				restClient.post()
				.uri(properties.getBaseUrl() + "/creditCard/" + API_NAME)
				.contentType(MediaType.APPLICATION_JSON)
				.body(requestFactory.create(
					API_NAME,
					properties.getInstitutionCode(),
					properties.getFintechAppNo(),
					apiKey,
					request.userKey(),
					new CardTransactionDto.ApiRequest(
						request.cardNo(),
						request.cvc(),
						request.startDate(),
						request.endDate()
					)
				))
				.retrieve()
				.body(responseType)
			).payload();
		} catch (SsafyApiException exception) {
			throw new TransactionException(resolveErrorCode(exception), exception);
		} catch (RestClientException exception) {
			throw new TransactionException(TransactionErrorCode.CARD_TRANSACTION_API_CALL_FAILED, exception);
		}

		if (record == null || record.transactionList() == null) {
			return new CardTransactionDto.Response(
				record != null ? record.cardIssuerCode() : null,
				record != null ? record.cardIssuerName() : null,
				record != null ? record.cardName() : null,
				record != null ? record.cardNo() : null,
				record != null ? record.estimatedBalance() : null,
				List.of()
			);
		}

		return new CardTransactionDto.Response(
			record.cardIssuerCode(),
			record.cardIssuerName(),
			record.cardName(),
			record.cardNo(),
			record.estimatedBalance(),
			record.transactionList().stream()
				.map(item -> new CardTransactionDto.Item(
					item.transactionUniqueNo(),
					item.categoryId(),
					item.categoryName(),
					item.merchantId(),
					item.merchantName(),
					item.transactionDate(),
					item.transactionTime(),
					item.transactionBalance(),
					item.cardStatus(),
					item.billStatementsYn(),
					item.billStatementsStatus()
				))
				.toList()
		);
	}

	private void validateConfiguration() {
		if (isBlank(properties.getBaseUrl())
			|| isBlank(properties.getInstitutionCode())
			|| isBlank(properties.getFintechAppNo())
			|| isBlank(apiKey)) {
			throw new TransactionException(TransactionErrorCode.CARD_TRANSACTION_API_CALL_FAILED);
		}
	}

	private void validateRequest(CardTransactionDto.Request request) {
		if (request == null
			|| isBlank(request.userKey())
			|| isBlank(request.cardNo())
			|| isBlank(request.cvc())
			|| isBlank(request.startDate())
			|| isBlank(request.endDate())) {
			throw new TransactionException(TransactionErrorCode.CARD_TRANSACTION_API_CALL_FAILED);
		}
	}

	private TransactionErrorCode resolveErrorCode(SsafyApiException exception) {
		if (exception.reason() == SsafyApiException.Reason.INVALID_CONFIGURATION
			|| exception.reason() == SsafyApiException.Reason.CALL_FAILED) {
			return TransactionErrorCode.CARD_TRANSACTION_API_CALL_FAILED;
		}
		return TransactionErrorCode.INVALID_CARD_TRANSACTION_API_RESPONSE;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}

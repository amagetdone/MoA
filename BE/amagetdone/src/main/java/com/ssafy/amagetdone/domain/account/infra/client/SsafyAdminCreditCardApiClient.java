package com.ssafy.amagetdone.domain.account.infra.client;

import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.infra.client.dto.AdminCreditCardDto;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.CardTransactionDto;
import com.ssafy.amagetdone.global.infra.ssafy.envelope.SsafyResponseEnvelope;
import com.ssafy.amagetdone.global.infra.ssafy.exception.SsafyApiException;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyHttpErrorParser;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiRequestFactory;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiResponseValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class SsafyAdminCreditCardApiClient {

	private static final Logger log = LoggerFactory.getLogger(SsafyAdminCreditCardApiClient.class);

	private static final String CREATE_CARD_API = "createCreditCard";
	private static final String CREATE_CARD_TRANSACTION_API = "createCreditCardTransaction";
	private static final String CANCEL_CARD_TRANSACTION_API = "deleteTransaction";

	private final RestClient restClient;
	private final AccountSsafyApiProperties properties;
	private final SsafyApiRequestFactory requestFactory;
	private final SsafyApiResponseValidator responseValidator;
	private final SsafyHttpErrorParser httpErrorParser;
	private final ParameterizedTypeReference<SsafyResponseEnvelope<ApiCreateCardResponse>> createCardResponseType;
	private final ParameterizedTypeReference<SsafyResponseEnvelope<ApiCreateCardTransactionResponse>> createCardTransactionResponseType;
	private final ParameterizedTypeReference<SsafyResponseEnvelope<ApiCancelCardTransactionResponse>> cancelCardTransactionResponseType;

	public SsafyAdminCreditCardApiClient(
		RestClient.Builder restClientBuilder,
		AccountSsafyApiProperties properties,
		SsafyApiRequestFactory requestFactory,
		SsafyApiResponseValidator responseValidator,
		SsafyHttpErrorParser httpErrorParser
	) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
		this.requestFactory = requestFactory;
		this.responseValidator = responseValidator;
		this.httpErrorParser = httpErrorParser;
		this.createCardResponseType = new ParameterizedTypeReference<>() {
		};
		this.createCardTransactionResponseType = new ParameterizedTypeReference<>() {
		};
		this.cancelCardTransactionResponseType = new ParameterizedTypeReference<>() {
		};
	}

	public AdminCreditCardDto.CreatedCard createCard(
		String apiKey,
		String userKey,
		String cardUniqueNo,
		String withdrawalAccountNo,
		String withdrawalDate
	) {
		if (isBlank(apiKey) || isBlank(userKey) || isBlank(cardUniqueNo) || isBlank(withdrawalAccountNo) || isBlank(withdrawalDate)) {
			throw new AccountException(AccountErrorCode.CREDIT_CARD_API_CALL_FAILED);
		}

		ApiCreateCardResponse payload = post(
			CREATE_CARD_API,
			apiKey,
			userKey,
			new CreateCardApiRequest(cardUniqueNo, withdrawalAccountNo, withdrawalDate),
			createCardResponseType,
			AccountErrorCode.CREDIT_CARD_API_CALL_FAILED,
			AccountErrorCode.INVALID_CREDIT_CARD_API_RESPONSE
		);

		return new AdminCreditCardDto.CreatedCard(
			payload.cardNo(),
			payload.cvc(),
			payload.cardUniqueNo(),
			payload.cardIssuerCode(),
			payload.cardIssuerName(),
			payload.cardName(),
			payload.baselinePerformance(),
			payload.maxBenefitLimit(),
			payload.cardDescription(),
			payload.cardExpiryDate(),
			payload.withdrawalAccountNo(),
			payload.withdrawalDate()
		);
	}

	public CardTransactionDto.Item createCardTransaction(
		String apiKey,
		String userKey,
		String cardNo,
		String cvc,
		Long merchantId,
		Long paymentBalance
	) {
		if (isBlank(apiKey) || isBlank(userKey) || isBlank(cardNo) || isBlank(cvc) || merchantId == null || paymentBalance == null) {
			throw new AccountException(AccountErrorCode.CREDIT_CARD_TRANSACTION_API_CALL_FAILED);
		}

		ApiCreateCardTransactionResponse payload = post(
			CREATE_CARD_TRANSACTION_API,
			apiKey,
			userKey,
			new CreateCardTransactionApiRequest(cardNo, cvc, merchantId, paymentBalance),
			createCardTransactionResponseType,
			AccountErrorCode.CREDIT_CARD_TRANSACTION_API_CALL_FAILED,
			AccountErrorCode.INVALID_CREDIT_CARD_TRANSACTION_API_RESPONSE
		);

		return new CardTransactionDto.Item(
			payload.transactionUniqueNo(),
			payload.categoryId(),
			payload.categoryName(),
			payload.merchantId(),
			payload.merchantName(),
			payload.transactionDate(),
			payload.transactionTime(),
			payload.paymentBalance(),
			"Y",
			"Y",
			"BILLED"
		);
	}

	public CardTransactionDto.Item cancelCardTransaction(
		String apiKey,
		String userKey,
		String cardNo,
		String cvc,
		Long transactionUniqueNo
	) {
		if (isBlank(apiKey) || isBlank(userKey) || isBlank(cardNo) || isBlank(cvc) || transactionUniqueNo == null) {
			throw new AccountException(AccountErrorCode.CREDIT_CARD_TRANSACTION_CANCEL_API_CALL_FAILED);
		}

		ApiCancelCardTransactionResponse payload = post(
			CANCEL_CARD_TRANSACTION_API,
			apiKey,
			userKey,
			new CancelCardTransactionApiRequest(cardNo, cvc, transactionUniqueNo),
			cancelCardTransactionResponseType,
			AccountErrorCode.CREDIT_CARD_TRANSACTION_CANCEL_API_CALL_FAILED,
			AccountErrorCode.INVALID_CREDIT_CARD_TRANSACTION_CANCEL_API_RESPONSE
		);

		return new CardTransactionDto.Item(
			payload.transactionUniqueNo(),
			payload.categoryId(),
			payload.categoryName(),
			payload.merchantId(),
			payload.merchantName(),
			payload.transactionDate(),
			payload.transactionTime(),
			payload.transactionBalance(),
			"Y",
			"Y",
			payload.status()
		);
	}

	private <T, R> R post(
		String apiName,
		String apiKey,
		String userKey,
		T body,
		ParameterizedTypeReference<SsafyResponseEnvelope<R>> responseType,
		AccountErrorCode callErrorCode,
		AccountErrorCode invalidResponseErrorCode
	) {
		if (isBlank(properties.getBaseUrl()) || isBlank(properties.getInstitutionCode()) || isBlank(properties.getFintechAppNo())) {
			throw new AccountException(callErrorCode);
		}

		try {
			R payload = responseValidator.validate(
				restClient.post()
					.uri(properties.getBaseUrl() + "/creditCard/" + apiName)
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
				throw new AccountException(invalidResponseErrorCode);
			}
			return payload;
		} catch (SsafyApiException exception) {
			if (exception.reason() == SsafyApiException.Reason.INVALID_CONFIGURATION
				|| exception.reason() == SsafyApiException.Reason.CALL_FAILED) {
				throw new AccountException(callErrorCode, exception);
			}
			throw new AccountException(invalidResponseErrorCode, exception);
		} catch (HttpClientErrorException exception) {
			SsafyHttpErrorParser.ParsedError parsedError = httpErrorParser.parse(exception.getResponseBodyAsString());
			log.error(
				"SSAFY credit card API client error. apiName={}, statusCode={}, responseCode={}, responseMessage={}, hasApiKey={}, hasUserKey={}, userKeyLength={}",
				apiName,
				exception.getStatusCode(),
				parsedError.responseCode(),
				parsedError.responseMessage(),
				!isBlank(apiKey),
				!isBlank(userKey),
				userKey == null ? 0 : userKey.length()
			);
			if (parsedError.responseCode() != null || parsedError.responseMessage() != null) {
				throw new AccountException(
					callErrorCode,
					new SsafyApiException(
						SsafyApiException.Reason.FAILED_RESPONSE_CODE,
						parsedError.responseCode(),
						parsedError.responseMessage()
					)
				);
			}
			throw new AccountException(callErrorCode, exception);
		} catch (RestClientException exception) {
			log.error(
				"SSAFY credit card API call failed. apiName={}, hasApiKey={}, hasUserKey={}, userKeyLength={}",
				apiName,
				!isBlank(apiKey),
				!isBlank(userKey),
				userKey == null ? 0 : userKey.length(),
				exception
			);
			throw new AccountException(callErrorCode, exception);
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private record CreateCardApiRequest(
		String cardUniqueNo,
		String withdrawalAccountNo,
		String withdrawalDate
	) {
	}

	private record CreateCardTransactionApiRequest(
		String cardNo,
		String cvc,
		Long merchantId,
		Long paymentBalance
	) {
	}

	private record CancelCardTransactionApiRequest(
		String cardNo,
		String cvc,
		Long transactionUniqueNo
	) {
	}

	private record ApiCreateCardResponse(
		String cardNo,
		String cvc,
		String cardUniqueNo,
		String cardIssuerCode,
		String cardIssuerName,
		String cardName,
		Long baselinePerformance,
		Long maxBenefitLimit,
		String cardDescription,
		String cardExpiryDate,
		String withdrawalAccountNo,
		String withdrawalDate
	) {
	}

	private record ApiCreateCardTransactionResponse(
		Long transactionUniqueNo,
		String categoryId,
		String categoryName,
		Long merchantId,
		String merchantName,
		String transactionDate,
		String transactionTime,
		Long paymentBalance
	) {
	}

	private record ApiCancelCardTransactionResponse(
		Long transactionUniqueNo,
		String categoryId,
		String categoryName,
		Long merchantId,
		String merchantName,
		String transactionDate,
		String transactionTime,
		Long transactionBalance,
		String status
	) {
	}
}

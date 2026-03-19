package com.ssafy.amagetdone.domain.account.infra.client;

import java.util.Comparator;
import java.util.List;

import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositTransferDto;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.DemandDepositTransactionDto;
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
public class SsafyAdminDemandDepositApiClient {

	private static final Logger log = LoggerFactory.getLogger(SsafyAdminDemandDepositApiClient.class);

	private static final String CREATE_ACCOUNT_API = "createDemandDepositAccount";
	private static final String DEPOSIT_API = "updateDemandDepositAccountDeposit";
	private static final String TRANSFER_API = "updateDemandDepositAccountTransfer";
	private static final String INQUIRE_TRANSACTIONS_API = "inquireTransactionHistoryList";

	private final RestClient restClient;
	private final AccountSsafyApiProperties properties;
	private final SsafyApiRequestFactory requestFactory;
	private final SsafyApiResponseValidator responseValidator;
	private final SsafyHttpErrorParser httpErrorParser;
	private final ParameterizedTypeReference<SsafyResponseEnvelope<ApiAccountCreateResponse>> createAccountResponseType;
	private final ParameterizedTypeReference<SsafyResponseEnvelope<DemandDepositTransferDto.DepositResponse>> depositResponseType;
	private final ParameterizedTypeReference<SsafyResponseEnvelope<List<DemandDepositTransferDto.TransferItem>>> transferResponseType;
	private final ParameterizedTypeReference<SsafyResponseEnvelope<DemandDepositTransactionDto.ApiResponse>> transactionResponseType;

	public SsafyAdminDemandDepositApiClient(
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
		this.createAccountResponseType = new ParameterizedTypeReference<>() {
		};
		this.depositResponseType = new ParameterizedTypeReference<>() {
		};
		this.transferResponseType = new ParameterizedTypeReference<>() {
		};
		this.transactionResponseType = new ParameterizedTypeReference<>() {
		};
	}

	public AccountCreationResult createAccount(String apiKey, String userKey, String accountTypeUniqueNo) {
		if (isBlank(userKey) || isBlank(apiKey) || isBlank(accountTypeUniqueNo)) {
			throw new AccountException(AccountErrorCode.DEMAND_DEPOSIT_ACCOUNT_API_CALL_FAILED);
		}

		ApiAccountCreateResponse payload = post(
			CREATE_ACCOUNT_API,
			apiKey,
			userKey,
			new CreateAccountApiRequest(accountTypeUniqueNo),
			createAccountResponseType,
			AccountErrorCode.DEMAND_DEPOSIT_ACCOUNT_API_CALL_FAILED,
			AccountErrorCode.INVALID_DEMAND_DEPOSIT_ACCOUNT_API_RESPONSE
		);

		if (payload.currency() == null) {
			throw new AccountException(AccountErrorCode.INVALID_DEMAND_DEPOSIT_ACCOUNT_API_RESPONSE);
		}

		return new AccountCreationResult(
			payload.bankCode(),
			payload.accountNo(),
			payload.currency().currency(),
			payload.currency().currencyName()
		);
	}

	public AccountCreationResult createHelperAccount(String apiKey, String userKey, String accountTypeUniqueNo) {
		return createAccount(apiKey, userKey, accountTypeUniqueNo);
	}

	public DemandDepositTransactionDto.Item createDepositTransaction(
		String apiKey,
		String userKey,
		String accountNo,
		Long transactionBalance,
		String transactionSummary
	) {
		if (isBlank(userKey) || isBlank(apiKey) || isBlank(accountNo) || transactionBalance == null) {
			throw new AccountException(AccountErrorCode.DEMAND_DEPOSIT_DEPOSIT_API_CALL_FAILED);
		}

		DemandDepositTransferDto.DepositResponse response = post(
			DEPOSIT_API,
			apiKey,
			userKey,
			new DepositApiRequest(accountNo, transactionBalance, transactionSummary),
			depositResponseType,
			AccountErrorCode.DEMAND_DEPOSIT_DEPOSIT_API_CALL_FAILED,
			AccountErrorCode.INVALID_DEMAND_DEPOSIT_DEPOSIT_API_RESPONSE
		);

		return findTransaction(apiKey, userKey, accountNo, response.transactionDate(), response.transactionUniqueNo());
	}

	public DemandDepositTransactionDto.Item createTransferTransaction(
		String apiKey,
		String userKey,
		String withdrawalAccountNo,
		String depositAccountNo,
		Long transactionBalance,
		String withdrawalTransactionSummary,
		String depositTransactionSummary
	) {
		if (isBlank(userKey)
			|| isBlank(apiKey)
			|| isBlank(withdrawalAccountNo)
			|| isBlank(depositAccountNo)
			|| transactionBalance == null) {
			throw new AccountException(AccountErrorCode.DEMAND_DEPOSIT_TRANSFER_API_CALL_FAILED);
		}

		List<DemandDepositTransferDto.TransferItem> response = post(
			TRANSFER_API,
			apiKey,
			userKey,
			new TransferApiRequest(
				depositAccountNo,
				transactionBalance,
				withdrawalAccountNo,
				depositTransactionSummary,
				withdrawalTransactionSummary
			),
			transferResponseType,
			AccountErrorCode.DEMAND_DEPOSIT_TRANSFER_API_CALL_FAILED,
			AccountErrorCode.INVALID_DEMAND_DEPOSIT_TRANSFER_API_RESPONSE
		);

		DemandDepositTransferDto.TransferItem withdrawalItem = response.stream()
			.filter(item -> withdrawalAccountNo.equals(item.accountNo()))
			.min(Comparator.comparing(DemandDepositTransferDto.TransferItem::transactionUniqueNo))
			.orElseThrow(() -> new AccountException(AccountErrorCode.INVALID_DEMAND_DEPOSIT_TRANSFER_API_RESPONSE));

		return findTransaction(apiKey, userKey, withdrawalAccountNo, withdrawalItem.transactionDate(), withdrawalItem.transactionUniqueNo());
	}

	private DemandDepositTransactionDto.Item findTransaction(
		String apiKey,
		String userKey,
		String accountNo,
		String transactionDate,
		Long transactionUniqueNo
	) {
		DemandDepositTransactionDto.ApiResponse payload = post(
			INQUIRE_TRANSACTIONS_API,
			apiKey,
			userKey,
			new InquireTransactionsApiRequest(accountNo, transactionDate, transactionDate, "A", "DESC"),
			transactionResponseType,
			AccountErrorCode.DEMAND_DEPOSIT_TRANSFER_API_CALL_FAILED,
			AccountErrorCode.INVALID_DEMAND_DEPOSIT_TRANSFER_API_RESPONSE
		);

		return payload.list().stream()
			.filter(item -> transactionUniqueNo.equals(item.transactionUniqueNo()))
			.findFirst()
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
			.orElseThrow(() -> new AccountException(AccountErrorCode.INVALID_DEMAND_DEPOSIT_TRANSFER_API_RESPONSE));
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
				throw new AccountException(invalidResponseErrorCode);
			}
			return payload;
		} catch (SsafyApiException exception) {
			throw new AccountException(resolveErrorCode(exception, callErrorCode, invalidResponseErrorCode), exception);
		} catch (HttpClientErrorException exception) {
			SsafyHttpErrorParser.ParsedError parsedError = httpErrorParser.parse(exception.getResponseBodyAsString());
			log.error(
				"SSAFY demand deposit API client error. apiName={}, statusCode={}, responseCode={}, responseMessage={}, hasApiKey={}, hasUserKey={}, userKeyLength={}",
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
				"SSAFY demand deposit API call failed. apiName={}, hasApiKey={}, hasUserKey={}, userKeyLength={}",
				apiName,
				!isBlank(apiKey),
				!isBlank(userKey),
				userKey == null ? 0 : userKey.length(),
				exception
			);
			throw new AccountException(callErrorCode, exception);
		}
	}

	private AccountErrorCode resolveErrorCode(
		SsafyApiException exception,
		AccountErrorCode callErrorCode,
		AccountErrorCode invalidResponseErrorCode
	) {
		if (exception.reason() == SsafyApiException.Reason.INVALID_CONFIGURATION
			|| exception.reason() == SsafyApiException.Reason.CALL_FAILED) {
			return callErrorCode;
		}
		return invalidResponseErrorCode;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	public record AccountCreationResult(
		String bankCode,
		String accountNo,
		String currency,
		String currencyName
	) {
	}

	private record CreateAccountApiRequest(
		String accountTypeUniqueNo
	) {
	}

	private record DepositApiRequest(
		String accountNo,
		Long transactionBalance,
		String transactionSummary
	) {
	}

	private record TransferApiRequest(
		String depositAccountNo,
		Long transactionBalance,
		String withdrawalAccountNo,
		String depositTransactionSummary,
		String withdrawalTransactionSummary
	) {
	}

	private record InquireTransactionsApiRequest(
		String accountNo,
		String startDate,
		String endDate,
		String transactionType,
		String orderByType
	) {
	}

	private record ApiAccountCreateResponse(
		String bankCode,
		String accountNo,
		Currency currency
	) {
	}

	private record Currency(
		String currency,
		String currencyName
	) {
	}
}

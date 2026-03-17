package com.ssafy.amagetdone.domain.account.infra.client;

import java.util.List;

import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositAccountDto;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositBalanceDto;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositTransferDto;
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
public class SsafyDemandDepositAccountApiClient {

	private static final String CREATE_ACCOUNT_API = "createDemandDepositAccount";
	private static final String LIST_ACCOUNT_API = "inquireDemandDepositAccountList";
	private static final String DETAIL_ACCOUNT_API = "inquireDemandDepositAccount";
	private static final String BALANCE_API = "inquireDemandDepositAccountBalance";
	private static final String DEPOSIT_API = "updateDemandDepositAccountDeposit";
	private static final String TRANSFER_API = "updateDemandDepositAccountTransfer";

	private final RestClient restClient;
	private final AccountSsafyApiProperties properties;
	private final SsafyApiRequestFactory requestFactory;
	private final SsafyApiResponseValidator responseValidator;
	private final ParameterizedTypeReference<SsafyResponseEnvelope<DemandDepositAccountDto.ApiAccountCreateResponse>> createAccountResponseType;
	private final ParameterizedTypeReference<SsafyResponseEnvelope<List<DemandDepositAccountDto.AccountItem>>> accountListResponseType;
	private final ParameterizedTypeReference<SsafyResponseEnvelope<DemandDepositAccountDto.AccountDetailResponse>> accountDetailResponseType;
	private final ParameterizedTypeReference<SsafyResponseEnvelope<DemandDepositBalanceDto.Response>> balanceResponseType;
	private final ParameterizedTypeReference<SsafyResponseEnvelope<DemandDepositTransferDto.DepositResponse>> depositResponseType;
	private final ParameterizedTypeReference<SsafyResponseEnvelope<List<DemandDepositTransferDto.TransferItem>>> transferResponseType;

	@Value("${account.ssafy.api-key}")
	private String apiKey;

	public SsafyDemandDepositAccountApiClient(
		RestClient.Builder restClientBuilder,
		AccountSsafyApiProperties properties,
		SsafyApiRequestFactory requestFactory,
		SsafyApiResponseValidator responseValidator
	) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
		this.requestFactory = requestFactory;
		this.responseValidator = responseValidator;
		this.createAccountResponseType = new ParameterizedTypeReference<>() {
		};
		this.accountListResponseType = new ParameterizedTypeReference<>() {
		};
		this.accountDetailResponseType = new ParameterizedTypeReference<>() {
		};
		this.balanceResponseType = new ParameterizedTypeReference<>() {
		};
		this.depositResponseType = new ParameterizedTypeReference<>() {
		};
		this.transferResponseType = new ParameterizedTypeReference<>() {
		};
	}

	public DemandDepositAccountDto.AccountCreateResponse createAccount(DemandDepositAccountDto.AccountCreateRequest request) {
		requireUserRequest(request == null ? null : request.userKey());
		DemandDepositAccountDto.ApiAccountCreateResponse payload = post(
			CREATE_ACCOUNT_API,
			request.userKey(),
			new CreateAccountApiRequest(request.accountTypeUniqueNo()),
			createAccountResponseType,
			AccountErrorCode.DEMAND_DEPOSIT_ACCOUNT_API_CALL_FAILED,
			AccountErrorCode.INVALID_DEMAND_DEPOSIT_ACCOUNT_API_RESPONSE
		);

		if (payload.currency() == null) {
			throw new AccountException(AccountErrorCode.INVALID_DEMAND_DEPOSIT_ACCOUNT_API_RESPONSE);
		}

		return new DemandDepositAccountDto.AccountCreateResponse(
			payload.bankCode(),
			payload.accountNo(),
			payload.currency().currency(),
			payload.currency().currencyName()
		);
	}

	public DemandDepositAccountDto.AccountListResponse inquireAccounts(DemandDepositAccountDto.AccountListRequest request) {
		requireUserRequest(request == null ? null : request.userKey());
		List<DemandDepositAccountDto.AccountItem> payload = post(
			LIST_ACCOUNT_API,
			request.userKey(),
			new EmptyRequest(),
			accountListResponseType,
			AccountErrorCode.DEMAND_DEPOSIT_ACCOUNT_API_CALL_FAILED,
			AccountErrorCode.INVALID_DEMAND_DEPOSIT_ACCOUNT_API_RESPONSE
		);

		return new DemandDepositAccountDto.AccountListResponse(payload == null ? List.of() : payload);
	}

	public DemandDepositAccountDto.AccountDetailResponse inquireAccount(DemandDepositAccountDto.AccountDetailRequest request) {
		requireUserRequest(request == null ? null : request.userKey());
		if (request == null || isBlank(request.accountNo())) {
			throw new AccountException(AccountErrorCode.DEMAND_DEPOSIT_ACCOUNT_API_CALL_FAILED);
		}

		return post(
			DETAIL_ACCOUNT_API,
			request.userKey(),
			new AccountDetailApiRequest(request.accountNo()),
			accountDetailResponseType,
			AccountErrorCode.DEMAND_DEPOSIT_ACCOUNT_API_CALL_FAILED,
			AccountErrorCode.INVALID_DEMAND_DEPOSIT_ACCOUNT_API_RESPONSE
		);
	}

	public DemandDepositBalanceDto.Response inquireBalance(DemandDepositBalanceDto.Request request) {
		requireUserRequest(request == null ? null : request.userKey());
		if (request == null || isBlank(request.accountNo())) {
			throw new AccountException(AccountErrorCode.DEMAND_DEPOSIT_BALANCE_API_CALL_FAILED);
		}

		return post(
			BALANCE_API,
			request.userKey(),
			new BalanceApiRequest(request.accountNo()),
			balanceResponseType,
			AccountErrorCode.DEMAND_DEPOSIT_BALANCE_API_CALL_FAILED,
			AccountErrorCode.INVALID_DEMAND_DEPOSIT_BALANCE_API_RESPONSE
		);
	}

	public DemandDepositTransferDto.DepositResponse deposit(DemandDepositTransferDto.DepositRequest request) {
		requireUserRequest(request == null ? null : request.userKey());
		if (request == null || isBlank(request.accountNo()) || request.transactionBalance() == null) {
			throw new AccountException(AccountErrorCode.DEMAND_DEPOSIT_DEPOSIT_API_CALL_FAILED);
		}

		return post(
			DEPOSIT_API,
			request.userKey(),
			new DepositApiRequest(request.accountNo(), request.transactionBalance(), request.transactionSummary()),
			depositResponseType,
			AccountErrorCode.DEMAND_DEPOSIT_DEPOSIT_API_CALL_FAILED,
			AccountErrorCode.INVALID_DEMAND_DEPOSIT_DEPOSIT_API_RESPONSE
		);
	}

	public DemandDepositTransferDto.TransferResponse transfer(DemandDepositTransferDto.TransferRequest request) {
		requireUserRequest(request == null ? null : request.userKey());
		if (request == null
			|| isBlank(request.withdrawalAccountNo())
			|| isBlank(request.depositAccountNo())
			|| request.transactionBalance() == null) {
			throw new AccountException(AccountErrorCode.DEMAND_DEPOSIT_TRANSFER_API_CALL_FAILED);
		}

		List<DemandDepositTransferDto.TransferItem> payload = post(
			TRANSFER_API,
			request.userKey(),
			new TransferApiRequest(
				request.depositAccountNo(),
				request.transactionBalance(),
				request.withdrawalAccountNo(),
				request.depositTransactionSummary(),
				request.withdrawalTransactionSummary()
			),
			transferResponseType,
			AccountErrorCode.DEMAND_DEPOSIT_TRANSFER_API_CALL_FAILED,
			AccountErrorCode.INVALID_DEMAND_DEPOSIT_TRANSFER_API_RESPONSE
		);

		return new DemandDepositTransferDto.TransferResponse(payload == null ? List.of() : payload);
	}

	private <T, R> R post(
		String apiName,
		String userKey,
		T body,
		ParameterizedTypeReference<SsafyResponseEnvelope<R>> responseType,
		AccountErrorCode callErrorCode,
		AccountErrorCode invalidResponseCode
	) {
		validateConfiguration(callErrorCode);

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

	private void requireUserRequest(String userKey) {
		if (isBlank(userKey)) {
			throw new AccountException(AccountErrorCode.USER_KEY_REQUIRED);
		}
	}

	private void validateConfiguration(AccountErrorCode errorCode) {
		if (isBlank(properties.getBaseUrl())
			|| isBlank(properties.getInstitutionCode())
			|| isBlank(properties.getFintechAppNo())
			|| isBlank(apiKey)) {
			throw new AccountException(errorCode);
		}
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

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private record EmptyRequest() {
	}

	private record CreateAccountApiRequest(
		String accountTypeUniqueNo
	) {
	}

	private record AccountDetailApiRequest(
		String accountNo
	) {
	}

	private record BalanceApiRequest(
		String accountNo
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
}

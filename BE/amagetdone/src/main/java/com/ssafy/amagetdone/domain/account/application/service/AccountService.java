package com.ssafy.amagetdone.domain.account.application.service;

import java.util.List;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.api.dto.AccountDto;
import com.ssafy.amagetdone.domain.account.application.usecase.FindUserAccountUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.RegisterAccountUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.CreateDemandDepositAccountUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.DepositAccountUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.InquireAccountBalanceUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.ListAccountTransactionsUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.ListAccountsUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.ListDemandDepositProductsUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.RegisterDemandDepositProductUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.SyncAccountsUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.TransferAccountUseCase;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositBalanceDto;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositTransferDto;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.DemandDepositTransactionDto;

import org.springframework.stereotype.Service;

@Service
public class AccountService {

	private final FindUserAccountUseCase findUserAccountUseCase;
	private final RegisterAccountUseCase registerAccountUseCase;
	private final RegisterDemandDepositProductUseCase registerDemandDepositProductUseCase;
	private final ListDemandDepositProductsUseCase listDemandDepositProductsUseCase;
	private final CreateDemandDepositAccountUseCase createDemandDepositAccountUseCase;
	private final SyncAccountsUseCase syncAccountsUseCase;
	private final ListAccountsUseCase listAccountsUseCase;
	private final InquireAccountBalanceUseCase inquireAccountBalanceUseCase;
	private final DepositAccountUseCase depositAccountUseCase;
	private final TransferAccountUseCase transferAccountUseCase;
	private final ListAccountTransactionsUseCase listAccountTransactionsUseCase;

	public AccountService(
		FindUserAccountUseCase findUserAccountUseCase,
		RegisterAccountUseCase registerAccountUseCase,
		RegisterDemandDepositProductUseCase registerDemandDepositProductUseCase,
		ListDemandDepositProductsUseCase listDemandDepositProductsUseCase,
		CreateDemandDepositAccountUseCase createDemandDepositAccountUseCase,
		SyncAccountsUseCase syncAccountsUseCase,
		ListAccountsUseCase listAccountsUseCase,
		InquireAccountBalanceUseCase inquireAccountBalanceUseCase,
		DepositAccountUseCase depositAccountUseCase,
		TransferAccountUseCase transferAccountUseCase,
		ListAccountTransactionsUseCase listAccountTransactionsUseCase
	) {
		this.findUserAccountUseCase = findUserAccountUseCase;
		this.registerAccountUseCase = registerAccountUseCase;
		this.registerDemandDepositProductUseCase = registerDemandDepositProductUseCase;
		this.listDemandDepositProductsUseCase = listDemandDepositProductsUseCase;
		this.createDemandDepositAccountUseCase = createDemandDepositAccountUseCase;
		this.syncAccountsUseCase = syncAccountsUseCase;
		this.listAccountsUseCase = listAccountsUseCase;
		this.inquireAccountBalanceUseCase = inquireAccountBalanceUseCase;
		this.depositAccountUseCase = depositAccountUseCase;
		this.transferAccountUseCase = transferAccountUseCase;
		this.listAccountTransactionsUseCase = listAccountTransactionsUseCase;
	}

	public AccountDto.AccountSummaryResponse registerAccount(Integer userId, AccountDto.RegisterAccountRequest request) {
		return registerAccountUseCase.execute(userId, request);
	}

	public AccountAdminDto.ProductResponse registerProduct(AccountAdminDto.ProductCreateRequest request) {
		return registerDemandDepositProductUseCase.execute(request);
	}

	public List<AccountAdminDto.ProductResponse> listProducts() {
		return listDemandDepositProductsUseCase.execute();
	}

	public AccountAdminDto.AccountCreateResponse createAccount(AccountAdminDto.AccountCreateRequest request) {
		return createDemandDepositAccountUseCase.execute(request);
	}

	public List<AccountDto.AccountSummaryResponse> syncAccounts(Integer userId) {
		List<Account> accounts = syncAccountsUseCase.execute(userId);
		return listAccountsUseCase.toResponses(accounts);
	}

	public AccountDto.AccountSummaryResponse getAccount(Integer userId, String accountNo) {
		Account account = findUserAccountUseCase.execute(userId, accountNo);
		return new AccountDto.AccountSummaryResponse(
			account.getAccountNo(),
			account.getBankCode(),
			account.getBankName(),
			account.getAccountName(),
			account.getAlias(),
			account.isVerified()
		);
	}

	public List<AccountDto.AccountSummaryResponse> getAccounts(Integer userId) {
		return listAccountsUseCase.toResponses(listAccountsUseCase.execute(userId));
	}

	public AccountDto.AccountBalanceResponse getBalance(Integer userId, String accountNo) {
		DemandDepositBalanceDto.Response response = inquireAccountBalanceUseCase.execute(userId, accountNo);
		return new AccountDto.AccountBalanceResponse(
			response.accountNo(),
			response.accountBalance(),
			response.accountCreatedDate(),
			response.accountExpiryDate(),
			response.lastTransactionDate(),
			response.currency()
		);
	}

	public AccountDto.DepositResponse deposit(Integer userId, AccountDto.DepositRequest request) {
		DemandDepositTransferDto.DepositResponse response = depositAccountUseCase.execute(userId, request);
		return new AccountDto.DepositResponse(response.transactionUniqueNo(), response.transactionDate());
	}

	public AccountDto.TransferResponse transfer(Integer userId, AccountDto.TransferRequest request) {
		DemandDepositTransferDto.TransferResponse response = transferAccountUseCase.execute(userId, request);
		return new AccountDto.TransferResponse(
			response.transactions().stream()
				.map(item -> new AccountDto.TransferItemResponse(
					item.transactionUniqueNo(),
					item.accountNo(),
					item.transactionDate(),
					item.transactionType(),
					item.transactionTypeName(),
					item.transactionAccountNo()
				))
				.toList()
		);
	}

	public AccountDto.TransactionHistoryResponse getTransactions(
		Integer userId,
		String accountNo,
		String startDate,
		String endDate,
		String transactionType,
		String orderByType
	) {
		DemandDepositTransactionDto.Response response = listAccountTransactionsUseCase.execute(
			userId,
			accountNo,
			startDate,
			endDate,
			transactionType,
			orderByType
		);

		return new AccountDto.TransactionHistoryResponse(
			response.totalCount(),
			response.transactions().stream()
				.map(item -> new AccountDto.TransactionItemResponse(
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
}

package com.ssafy.amagetdone.domain.account.application.service;

import java.util.List;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.api.dto.AccountDto;
import com.ssafy.amagetdone.domain.account.application.usecase.ConfirmAccountOwnershipUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.FindAccountUserUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.FindUserAccountUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.CreateDemandDepositAccountUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.ListAccountsUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.ListDemandDepositProductsUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.ReconcileLinkedAccountsUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.RegisterDemandDepositProductUseCase;
import com.ssafy.amagetdone.domain.account.infra.client.SsafyDemandDepositAccountApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositAccountDto;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.account.model.entity.Account;

import org.springframework.stereotype.Service;

@Service
public class AccountService {

	private final FindAccountUserUseCase findAccountUserUseCase;
	private final FindUserAccountUseCase findUserAccountUseCase;
	private final ConfirmAccountOwnershipUseCase confirmAccountOwnershipUseCase;
	private final SsafyDemandDepositAccountApiClient ssafyDemandDepositAccountApiClient;
	private final RegisterDemandDepositProductUseCase registerDemandDepositProductUseCase;
	private final ListDemandDepositProductsUseCase listDemandDepositProductsUseCase;
	private final CreateDemandDepositAccountUseCase createDemandDepositAccountUseCase;
	private final ListAccountsUseCase listAccountsUseCase;
	private final ReconcileLinkedAccountsUseCase reconcileLinkedAccountsUseCase;

	public AccountService(
		FindAccountUserUseCase findAccountUserUseCase,
		FindUserAccountUseCase findUserAccountUseCase,
		ConfirmAccountOwnershipUseCase confirmAccountOwnershipUseCase,
		SsafyDemandDepositAccountApiClient ssafyDemandDepositAccountApiClient,
		RegisterDemandDepositProductUseCase registerDemandDepositProductUseCase,
		ListDemandDepositProductsUseCase listDemandDepositProductsUseCase,
		CreateDemandDepositAccountUseCase createDemandDepositAccountUseCase,
		ListAccountsUseCase listAccountsUseCase,
		ReconcileLinkedAccountsUseCase reconcileLinkedAccountsUseCase
	) {
		this.findAccountUserUseCase = findAccountUserUseCase;
		this.findUserAccountUseCase = findUserAccountUseCase;
		this.confirmAccountOwnershipUseCase = confirmAccountOwnershipUseCase;
		this.ssafyDemandDepositAccountApiClient = ssafyDemandDepositAccountApiClient;
		this.registerDemandDepositProductUseCase = registerDemandDepositProductUseCase;
		this.listDemandDepositProductsUseCase = listDemandDepositProductsUseCase;
		this.createDemandDepositAccountUseCase = createDemandDepositAccountUseCase;
		this.listAccountsUseCase = listAccountsUseCase;
		this.reconcileLinkedAccountsUseCase = reconcileLinkedAccountsUseCase;
	}

	public List<AccountDto.BankAccountResponse> getBankAccounts(Integer userId) {
		User user = findAccountUserUseCase.execute(userId);
		DemandDepositAccountDto.AccountListResponse response = ssafyDemandDepositAccountApiClient.inquireAccounts(
			new DemandDepositAccountDto.AccountListRequest(user.getUserKey())
		);
		return response.accounts().stream()
			.map(item -> new AccountDto.BankAccountResponse(
				item.bankCode(),
				item.bankName(),
				item.accountNo(),
				item.accountName(),
				item.accountTypeName(),
				item.accountBalance(),
				item.currency()
			))
			.toList();
	}

	public AccountDto.ConfirmAccountResponse confirmAccount(Integer userId, AccountDto.ConfirmAccountRequest request) {
		return confirmAccountOwnershipUseCase.execute(userId, request);
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
		return listAccountsUseCase.toResponses(reconcileLinkedAccountsUseCase.execute(userId));
	}
}

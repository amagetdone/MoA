package com.ssafy.amagetdone.domain.account.application.service;

import java.util.List;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.application.usecase.InitializeAccountTransactionOverridesUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.InitializeCardTransactionOverridesUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.ListDemandDepositProductsUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.RegisterDemandDepositProductUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountAdminService {

	private final RegisterDemandDepositProductUseCase registerDemandDepositProductUseCase;
	private final ListDemandDepositProductsUseCase listDemandDepositProductsUseCase;
	private final InitializeAccountTransactionOverridesUseCase initializeAccountTransactionOverridesUseCase;
	private final InitializeCardTransactionOverridesUseCase initializeCardTransactionOverridesUseCase;

	public AccountAdminDto.ProductResponse registerProduct(AccountAdminDto.ProductCreateRequest request) {
		return registerDemandDepositProductUseCase.execute(request);
	}

	public List<AccountAdminDto.ProductResponse> listProducts() {
		return listDemandDepositProductsUseCase.execute();
	}

	public AccountAdminDto.TransactionInitResponse initializeAccountTransactions(
		AccountAdminDto.AccountTransactionInitRequest request
	) {
		return initializeAccountTransactionOverridesUseCase.execute(request);
	}

	public AccountAdminDto.TransactionInitResponse initializeCardTransactions(
		AccountAdminDto.CardTransactionInitRequest request
	) {
		return initializeCardTransactionOverridesUseCase.execute(request);
	}
}

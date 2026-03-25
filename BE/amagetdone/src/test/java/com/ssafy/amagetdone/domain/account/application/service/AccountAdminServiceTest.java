package com.ssafy.amagetdone.domain.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.application.usecase.InitializeAccountTransactionOverridesUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.InitializeCardTransactionOverridesUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.ListDemandDepositProductsUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.RegisterDemandDepositProductUseCase;

import org.junit.jupiter.api.Test;

class AccountAdminServiceTest {

	@Test
	void registerProductDelegatesToUseCase() {
		RegisterDemandDepositProductUseCase registerDemandDepositProductUseCase = mock(RegisterDemandDepositProductUseCase.class);
		ListDemandDepositProductsUseCase listDemandDepositProductsUseCase = mock(ListDemandDepositProductsUseCase.class);
		InitializeAccountTransactionOverridesUseCase initializeAccountTransactionOverridesUseCase =
			mock(InitializeAccountTransactionOverridesUseCase.class);
		InitializeCardTransactionOverridesUseCase initializeCardTransactionOverridesUseCase =
			mock(InitializeCardTransactionOverridesUseCase.class);
		AccountAdminService service = new AccountAdminService(
			registerDemandDepositProductUseCase,
			listDemandDepositProductsUseCase,
			initializeAccountTransactionOverridesUseCase,
			initializeCardTransactionOverridesUseCase
		);
		AccountAdminDto.ProductCreateRequest request = new AccountAdminDto.ProductCreateRequest("001", "모아 상품", "설명");
		AccountAdminDto.ProductResponse response = new AccountAdminDto.ProductResponse(
			"001-1-abc",
			"001",
			"테스트은행",
			"1",
			"수시입출금",
			"모아 상품",
			"설명",
			"DOMESTIC"
		);
		when(registerDemandDepositProductUseCase.execute(request)).thenReturn(response);

		assertThat(service.registerProduct(request)).isEqualTo(response);
		verify(registerDemandDepositProductUseCase).execute(request);
	}
}

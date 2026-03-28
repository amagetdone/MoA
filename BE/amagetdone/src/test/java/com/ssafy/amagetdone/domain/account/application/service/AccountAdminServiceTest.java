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
import com.ssafy.amagetdone.domain.plan.application.usecase.InitializePlanDataUseCase;

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
		InitializePlanDataUseCase initializePlanDataUseCase = mock(InitializePlanDataUseCase.class);
		AccountAdminService service = new AccountAdminService(
			registerDemandDepositProductUseCase,
			listDemandDepositProductsUseCase,
			initializeAccountTransactionOverridesUseCase,
			initializeCardTransactionOverridesUseCase,
			initializePlanDataUseCase
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

	@Test
	void initializePlanDataDelegatesToUseCase() {
		RegisterDemandDepositProductUseCase registerDemandDepositProductUseCase = mock(RegisterDemandDepositProductUseCase.class);
		ListDemandDepositProductsUseCase listDemandDepositProductsUseCase = mock(ListDemandDepositProductsUseCase.class);
		InitializeAccountTransactionOverridesUseCase initializeAccountTransactionOverridesUseCase =
			mock(InitializeAccountTransactionOverridesUseCase.class);
		InitializeCardTransactionOverridesUseCase initializeCardTransactionOverridesUseCase =
			mock(InitializeCardTransactionOverridesUseCase.class);
		InitializePlanDataUseCase initializePlanDataUseCase = mock(InitializePlanDataUseCase.class);
		AccountAdminService service = new AccountAdminService(
			registerDemandDepositProductUseCase,
			listDemandDepositProductsUseCase,
			initializeAccountTransactionOverridesUseCase,
			initializeCardTransactionOverridesUseCase,
			initializePlanDataUseCase
		);
		AccountAdminDto.PlanDataInitRequest request = new AccountAdminDto.PlanDataInitRequest(13);
		AccountAdminDto.PlanDataInitResponse response = new AccountAdminDto.PlanDataInitResponse(
			"plan",
			13,
			3,
			0,
			java.util.List.of("맥북 프로 구매", "여행 자금 모으기", "자격증 교재 구매"),
			java.util.List.of()
		);
		when(initializePlanDataUseCase.execute(request)).thenReturn(response);

		assertThat(service.initializePlanData(request)).isEqualTo(response);
		verify(initializePlanDataUseCase).execute(request);
	}
}

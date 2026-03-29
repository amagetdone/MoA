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
import com.ssafy.amagetdone.domain.plan.application.usecase.SeedDemoPlanDataUseCase;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

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
		SeedDemoPlanDataUseCase seedDemoPlanDataUseCase = mock(SeedDemoPlanDataUseCase.class);
		AccountAdminService service = new AccountAdminService(
			registerDemandDepositProductUseCase,
			listDemandDepositProductsUseCase,
			initializeAccountTransactionOverridesUseCase,
			initializeCardTransactionOverridesUseCase,
			initializePlanDataUseCase,
			seedDemoPlanDataUseCase
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
		SeedDemoPlanDataUseCase seedDemoPlanDataUseCase = mock(SeedDemoPlanDataUseCase.class);
		AccountAdminService service = new AccountAdminService(
			registerDemandDepositProductUseCase,
			listDemandDepositProductsUseCase,
			initializeAccountTransactionOverridesUseCase,
			initializeCardTransactionOverridesUseCase,
			initializePlanDataUseCase,
			seedDemoPlanDataUseCase
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

	@Test
	void seedDemoPlanDataDelegatesToUseCase() {
		RegisterDemandDepositProductUseCase registerDemandDepositProductUseCase = mock(RegisterDemandDepositProductUseCase.class);
		ListDemandDepositProductsUseCase listDemandDepositProductsUseCase = mock(ListDemandDepositProductsUseCase.class);
		InitializeAccountTransactionOverridesUseCase initializeAccountTransactionOverridesUseCase =
			mock(InitializeAccountTransactionOverridesUseCase.class);
		InitializeCardTransactionOverridesUseCase initializeCardTransactionOverridesUseCase =
			mock(InitializeCardTransactionOverridesUseCase.class);
		InitializePlanDataUseCase initializePlanDataUseCase = mock(InitializePlanDataUseCase.class);
		SeedDemoPlanDataUseCase seedDemoPlanDataUseCase = mock(SeedDemoPlanDataUseCase.class);
		AccountAdminService service = new AccountAdminService(
			registerDemandDepositProductUseCase,
			listDemandDepositProductsUseCase,
			initializeAccountTransactionOverridesUseCase,
			initializeCardTransactionOverridesUseCase,
			initializePlanDataUseCase,
			seedDemoPlanDataUseCase
		);
		AccountAdminDto.DemoPlanSeedRequest request = new AccountAdminDto.DemoPlanSeedRequest(
			12,
			java.util.List.of(
				new AccountAdminDto.DemoPlanSeedPlanRequest(
					"유럽 여행",
					"유럽 배낭 여행을 위한 저축",
					1_000_000L,
					1,
					PlanStatus.ACTIVE,
					java.time.LocalDate.of(2026, 3, 9),
					java.time.LocalDate.of(2026, 4, 26),
					java.time.LocalDate.of(2026, 4, 26),
					java.util.List.of(
						new AccountAdminDto.DemoPlanSeedWeekRequest(1, 125_000L, 125_000L, 125_000L)
					)
				)
			)
		);
		AccountAdminDto.DemoPlanSeedResponse response = new AccountAdminDto.DemoPlanSeedResponse(
			"demo-plan",
			12,
			1,
			1,
			1,
			125_000L,
			java.util.List.of(
				new AccountAdminDto.DemoPlanSeedPlanResult(
					170,
					"유럽 여행",
					PlanStatus.ACTIVE,
					125_000L,
					1_000_000L,
					java.time.LocalDate.of(2026, 3, 9),
					java.time.LocalDate.of(2026, 4, 26),
					java.time.LocalDate.of(2026, 4, 26),
					7
				)
			)
		);
		when(seedDemoPlanDataUseCase.execute(request)).thenReturn(response);

		assertThat(service.seedDemoPlanData(request)).isEqualTo(response);
		verify(seedDemoPlanDataUseCase).execute(request);
	}
}

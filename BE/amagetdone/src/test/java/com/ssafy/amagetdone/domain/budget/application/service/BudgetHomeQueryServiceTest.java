package com.ssafy.amagetdone.domain.budget.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.account.application.usecase.InquireAccountBalanceUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.ListAccountsUseCase;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositBalanceDto;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetHomeDto;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BudgetHomeQueryServiceTest {

	@Mock
	private BudgetWeekRepository budgetWeekRepository;

	@Mock
	private PlanRepository planRepository;

	@Mock
	private PlanWeekAllocationRepository planWeekAllocationRepository;

	@Mock
	private ListAccountsUseCase listAccountsUseCase;

	@Mock
	private InquireAccountBalanceUseCase inquireAccountBalanceUseCase;

	@Mock
	private EntityManager entityManager;

	private BudgetHomeQueryService service;

	@BeforeEach
	void setUp() {
		service = new BudgetHomeQueryService(
			budgetWeekRepository,
			planRepository,
			planWeekAllocationRepository,
			listAccountsUseCase,
			inquireAccountBalanceUseCase,
			entityManager
		);
	}

	@Test
	void getHomeReturnsLinkedAccountDetailsInRepositoryOrderAndPreservesOtherSections() {
		LocalDate today = LocalDate.now();
		User user = user(1);
		BudgetWeek currentWeek = budgetWeek(user, today.minusDays(1), today.plusDays(2), 120_000L, 45_000L);
		Plan travelFund = activePlan(user, 11, "Travel Fund", 1, 1_000_000L, 680_000L, today.plusDays(45));
		Plan weddingFund = activePlan(user, 12, "Wedding Fund", 2, 2_000_000L, 500_000L, today.plusDays(90));
		List<PlanWeekAllocation> allocations = List.of(
			allocation(currentWeek, travelFund, 100_000L),
			allocation(currentWeek, weddingFund, 80_000L)
		);
		Account firstAccount = linkedAccount(user, "111-111-111111", "Shinhan");
		Account secondAccount = linkedAccount(user, "222-222-222222", "KB");

		given(entityManager.getReference(User.class, 1)).willReturn(user);
		given(budgetWeekRepository.findByUserAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(user, today, today))
			.willReturn(Optional.of(currentWeek));
		given(planRepository.findAll()).willReturn(List.of(weddingFund, travelFund));
		given(planWeekAllocationRepository.findByBudgetWeek(currentWeek)).willReturn(allocations);
		given(listAccountsUseCase.execute(1)).willReturn(List.of(firstAccount, secondAccount));
		given(inquireAccountBalanceUseCase.execute(1, "111-111-111111"))
			.willReturn(balance("111-111-111111", 2_000_000L));
		given(inquireAccountBalanceUseCase.execute(1, "222-222-222222"))
			.willReturn(balance("222-222-222222", 3_400_000L));

		BudgetHomeDto.Response response = service.getHome(1);

		assertThat(response.freeSpending().usedAmount()).isEqualTo(45_000L);
		assertThat(response.freeSpending().totalAmount()).isEqualTo(120_000L);
		assertThat(response.freeSpending().remainingAmount()).isEqualTo(75_000L);
		assertThat(response.weeklySaving().thisWeekSavingAmount()).isEqualTo(180_000L);
		assertThat(response.weeklySaving().remainingToTargetAmount()).isEqualTo(1_820_000L);
		assertThat(response.weeklySaving().progressRate()).isEqualTo(39L);
		assertThat(response.plans()).extracting(BudgetHomeDto.PlanCard::title)
			.containsExactly("Travel Fund", "Wedding Fund");
		assertThat(response.assetSummary().totalAssetAmount()).isEqualTo(5_400_000L);
	}

	@Test
	void getHomeIncludesVerifiedFlagForEachAssetSummaryAccount() {
		LocalDate today = LocalDate.now();
		User user = user(1);
		BudgetWeek currentWeek = budgetWeek(user, today, today.plusDays(6), 90_000L, 15_000L);
		Account verifiedAccount = linkedAccount(user, "111-111-111111", "Shinhan");
		Account unverifiedAccount = unverifiedActiveAccount(user, "222-222-222222", "KB");

		given(entityManager.getReference(User.class, 1)).willReturn(user);
		given(budgetWeekRepository.findByUserAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(user, today, today))
			.willReturn(Optional.of(currentWeek));
		given(planRepository.findAll()).willReturn(List.of());
		given(planWeekAllocationRepository.findByBudgetWeek(currentWeek)).willReturn(List.of());
		given(listAccountsUseCase.execute(1)).willReturn(List.of(verifiedAccount, unverifiedAccount));
		given(inquireAccountBalanceUseCase.execute(1, "111-111-111111"))
			.willReturn(balance("111-111-111111", 2_000_000L));
		given(inquireAccountBalanceUseCase.execute(1, "222-222-222222"))
			.willReturn(balance("222-222-222222", 500_000L));

		BudgetHomeDto.Response response = service.getHome(1);

		assertThat(response.assetSummary().totalAssetAmount()).isEqualTo(2_500_000L);
	}

	@Test
	void getHomeReturnsZeroAssetSummaryWhenNoLinkedAccountsExist() {
		LocalDate today = LocalDate.now();
		User user = user(1);
		BudgetWeek currentWeek = budgetWeek(user, today, today.plusDays(6), 70_000L, 10_000L);

		given(entityManager.getReference(User.class, 1)).willReturn(user);
		given(budgetWeekRepository.findByUserAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(user, today, today))
			.willReturn(Optional.of(currentWeek));
		given(planRepository.findAll()).willReturn(List.of());
		given(planWeekAllocationRepository.findByBudgetWeek(currentWeek)).willReturn(List.of());
		given(listAccountsUseCase.execute(1)).willReturn(List.of());

		BudgetHomeDto.Response response = service.getHome(1);

		assertThat(response.assetSummary().totalAssetAmount()).isZero();
		assertThat(response.freeSpending().remainingAmount()).isEqualTo(60_000L);
		assertThat(response.weeklySaving().thisWeekSavingAmount()).isZero();
		assertThat(response.plans()).isEmpty();
	}

	private User user(int userId) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", userId);
		return user;
	}

	private BudgetWeek budgetWeek(User user, LocalDate startDate, LocalDate endDate, long finalFreeAmount, long actualSpentAmount) {
		BudgetWeek budgetWeek = BudgetWeek.create(null, user, 1, startDate, endDate);
		budgetWeek.setFinalFreeAmount(finalFreeAmount);
		budgetWeek.setActualSpentAmount(actualSpentAmount);
		return budgetWeek;
	}

	private Plan activePlan(
		User user,
		int planId,
		String title,
		int priority,
		long targetAmount,
		long savedAmount,
		LocalDate endDate
	) {
		Plan plan = Plan.builder()
			.user(user)
			.title(title)
			.targetAmount(targetAmount)
			.savedAmount(savedAmount)
			.startDate(endDate.minusDays(30))
			.endDate(endDate)
			.targetWeeks(10)
			.priority(priority)
			.status(PlanStatus.ACTIVE)
			.planUrl("https://example.com/" + title.toLowerCase().replace(" ", "-") + ".png")
			.build();
		ReflectionTestUtils.setField(plan, "id", planId);
		return plan;
	}

	private PlanWeekAllocation allocation(BudgetWeek budgetWeek, Plan plan, long finalAmount) {
		return PlanWeekAllocation.builder()
			.budgetWeek(budgetWeek)
			.plan(plan)
			.finalAmount(finalAmount)
			.build();
	}

	private Account linkedAccount(User user, String accountNo, String bankName) {
		Account account = Account.create(
			user,
			accountNo,
			"DEMAND",
			"입출금통장",
			"수시입출금",
			"001",
			bankName,
			"KRW"
		);
		account.markVerifiedLinked();
		return account;
	}

	private Account unverifiedActiveAccount(User user, String accountNo, String bankName) {
		return Account.create(
			user,
			accountNo,
			"DEMAND",
			"입출금통장",
			"수시입출금",
			"001",
			bankName,
			"KRW"
		);
	}

	private DemandDepositBalanceDto.Response balance(String accountNo, long amount) {
		return new DemandDepositBalanceDto.Response("001", accountNo, amount, null, null, null, "KRW");
	}
}

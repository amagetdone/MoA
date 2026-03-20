package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

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
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BudgetHomeQueryService {

	private final BudgetWeekRepository budgetWeekRepository;
	private final PlanRepository planRepository;
	private final PlanWeekAllocationRepository planWeekAllocationRepository;
	private final ListAccountsUseCase listAccountsUseCase;
	private final InquireAccountBalanceUseCase inquireAccountBalanceUseCase;
	private final EntityManager entityManager;

	public BudgetHomeDto.Response getHome(Integer userId) {
		LocalDate today = LocalDate.now();
		User userReference = entityManager.getReference(User.class, userId);
		BudgetWeek budgetWeek = budgetWeekRepository
			.findByUserAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(userReference, today, today)
			.orElseThrow(() -> new IllegalArgumentException("Current budget week not found"));

		List<Plan> selectedPlans = selectActivePlans(userId);
		List<PlanWeekAllocation> currentWeekAllocations = planWeekAllocationRepository.findByBudgetWeek(budgetWeek);

		return BudgetHomeDto.Response.of(
			toFreeSpending(budgetWeek, today),
			toWeeklySaving(currentWeekAllocations, selectedPlans),
			toAssetSummary(userId),
			toPlanCards(selectedPlans, today)
		);
	}

	private List<Plan> selectActivePlans(Integer userId) {
		return planRepository.findAll().stream()
			.filter(plan -> plan.getUser() != null && userId.equals(plan.getUser().getId()))
			.filter(plan -> plan.getStatus() == PlanStatus.ACTIVE)
			.sorted(Comparator.comparing(Plan::getPriority)
				.thenComparing(Plan::getTitle))
			.limit(3L)
			.toList();
	}

	private BudgetHomeDto.FreeSpending toFreeSpending(BudgetWeek budgetWeek, LocalDate today) {
		long totalAmount = zero(budgetWeek.getFinalFreeAmount());
		long usedAmount = zero(budgetWeek.getActualSpentAmount());
		long remainingAmount = totalAmount - usedAmount;
		long daysRemaining = Math.max(0L, ChronoUnit.DAYS.between(today, budgetWeek.getWeekEndDate()) + 1L);
		long dailyAvailable = daysRemaining > 0 ? remainingAmount / daysRemaining : 0L;

		return BudgetHomeDto.FreeSpending.of(
			usedAmount,
			totalAmount,
			remainingAmount,
			daysRemaining,
			dailyAvailable
		);
	}

	private BudgetHomeDto.WeeklySaving toWeeklySaving(
		List<PlanWeekAllocation> currentWeekAllocations,
		List<Plan> selectedPlans
	) {
		Set<Integer> selectedPlanIds = selectedPlans.stream()
			.map(Plan::getId)
			.filter(java.util.Objects::nonNull)
			.collect(java.util.stream.Collectors.toSet());

		long thisWeekSavingAmount = currentWeekAllocations.stream()
			.filter(allocation -> allocation.getPlan() != null)
			.filter(allocation -> selectedPlanIds.contains(allocation.getPlan().getId()))
			.mapToLong(allocation -> zero(allocation.getFinalAmount()))
			.sum();
		long remainingToTargetAmount = selectedPlans.stream()
			.mapToLong(plan -> zero(plan.getTargetAmount()) - zero(plan.getSavedAmount()))
			.sum();
		long totalSavedAmount = selectedPlans.stream()
			.mapToLong(plan -> zero(plan.getSavedAmount()))
			.sum();
		long totalTargetAmount = selectedPlans.stream()
			.mapToLong(plan -> zero(plan.getTargetAmount()))
			.sum();

		return BudgetHomeDto.WeeklySaving.of(
			thisWeekSavingAmount,
			remainingToTargetAmount,
			calculateProgressRate(totalSavedAmount, totalTargetAmount)
		);
	}

	private BudgetHomeDto.AssetSummary toAssetSummary(Integer userId) {
		List<BudgetHomeDto.AssetAccount> accounts = listAccountsUseCase.execute(userId).stream()
			.map(account -> toAssetAccount(userId, account))
			.toList();
		long totalAssetAmount = accounts.stream()
			.mapToLong(BudgetHomeDto.AssetAccount::balanceAmount)
			.sum();

		return BudgetHomeDto.AssetSummary.of(totalAssetAmount, accounts);
	}

	private BudgetHomeDto.AssetAccount toAssetAccount(Integer userId, Account account) {
		return BudgetHomeDto.AssetAccount.of(
			account.getId() == null ? account.getAccountNo() : account.getId(),
			account.getBankName(),
			account.getAccountNo(),
			inquireBalance(userId, account.getAccountNo()),
			account.isVerified()
		);
	}

	private long inquireBalance(Integer userId, String accountNo) {
		DemandDepositBalanceDto.Response response = inquireAccountBalanceUseCase.execute(userId, accountNo);
		return zero(response.accountBalance());
	}

	private List<BudgetHomeDto.PlanCard> toPlanCards(List<Plan> selectedPlans, LocalDate today) {
		return selectedPlans.stream()
			.map(plan -> BudgetHomeDto.PlanCard.of(
				plan.getId() == null ? null : Long.valueOf(plan.getId()),
				plan.getTitle(),
				plan.getPriority(),
				calculateDDay(today, plan.getEndDate()),
				zero(plan.getSavedAmount()),
				zero(plan.getTargetAmount()),
				calculateProgressRate(zero(plan.getSavedAmount()), zero(plan.getTargetAmount())),
				plan.getPlanUrl()
			))
			.toList();
	}

	private long calculateDDay(LocalDate today, LocalDate endDate) {
		if (endDate == null) {
			return 0L;
		}

		return ChronoUnit.DAYS.between(today, endDate);
	}

	private long calculateProgressRate(long savedAmount, long targetAmount) {
		if (targetAmount <= 0L) {
			return 0L;
		}

		return savedAmount * 100L / targetAmount;
	}

	private long zero(Long amount) {
		return amount == null ? 0L : amount;
	}
}

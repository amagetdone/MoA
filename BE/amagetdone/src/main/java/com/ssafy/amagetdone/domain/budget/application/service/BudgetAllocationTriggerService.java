package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import com.ssafy.amagetdone.domain.budget.application.usecase.AllocateBudgetUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.RefreshEssentialExpenseMonthFromAnalysisUseCase;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseMonth;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetMonthRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseMonthRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BudgetAllocationTriggerService {

	private final AllocateBudgetUseCase allocateBudgetUseCase;
	private final RefreshEssentialExpenseMonthFromAnalysisUseCase refreshEssentialExpenseMonthFromAnalysisUseCase;
	private final BudgetMonthRepository budgetMonthRepository;
	private final EssentialExpenseMonthRepository essentialExpenseMonthRepository;
	private final PlanRepository planRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	public void trigger(Integer userId) {
		LocalDate baseMonth = LocalDate.now(clock).withDayOfMonth(1);
		LocalDate serviceStartDate = baseMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
		LocalDate allocationEndDate = baseMonth.withDayOfMonth(baseMonth.lengthOfMonth())
			.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
			.plusDays(6L);
		triggerHistorical(userId, serviceStartDate, allocationEndDate);
	}

	public BudgetAllocationService.AllocationResult triggerHistorical(
		Integer userId,
		LocalDate serviceStartDate,
		LocalDate allocationEndDate
	) {
		if (userId == null || serviceStartDate == null || allocationEndDate == null || allocationEndDate.isBefore(serviceStartDate)) {
			return new BudgetAllocationService.AllocationResult(List.of(), List.of(), List.of(), List.of(), List.of());
		}
		userRepository.findByIdForUpdate(userId)
			.orElseThrow(() -> new IllegalArgumentException("User not found"));

		LocalDate baseMonth = serviceStartDate.withDayOfMonth(1);
		refreshEssentialExpenseMonthFromAnalysisUseCase.execute(userId, baseMonth);

		User userReference = userReference(userId);
		long monthlyIncome = resolveMonthlyIncome(userId, baseMonth);

		List<EssentialExpenseMonth> essentials = essentialExpenseMonthRepository
			.findAllByBudgetMonth_User_IdOrderByBudgetMonth_BaseDateAscIdAsc(userId);
		List<Plan> activePlans = planRepository.findAllByUser_IdAndStatusAndDeletedFalse(userId, PlanStatus.ACTIVE);

		return allocateBudgetUseCase.execute(
			userReference,
			serviceStartDate,
			monthlyIncome,
			essentials,
			activePlans,
			allocationEndDate
		);
	}

	private long resolveMonthlyIncome(Integer userId, LocalDate baseMonth) {
		return budgetMonthRepository.findFirstByUser_IdAndBaseDateOrderByIdDesc(userId, baseMonth)
			.or(() -> budgetMonthRepository.findFirstByUser_IdOrderByBaseDateDescIdDesc(userId))
			.map(BudgetMonth::getTotalIncomeAmount)
			.orElse(0L);
	}

	private User userReference(Integer userId) {
		User user = User.createPendingSocialUser();
		user.setId(userId);
		return user;
	}
}

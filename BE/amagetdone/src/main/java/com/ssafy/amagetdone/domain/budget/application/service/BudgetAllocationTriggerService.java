package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import com.ssafy.amagetdone.domain.budget.application.usecase.AllocateBudgetUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.RefreshEssentialExpenseMonthFromAnalysisUseCase;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseMonth;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetMonthRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseMonthRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
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
	private final Clock clock;

	public void trigger(Integer userId) {
		LocalDate baseMonth = LocalDate.now(clock).withDayOfMonth(1);
		refreshEssentialExpenseMonthFromAnalysisUseCase.execute(userId, baseMonth);

		User userReference = userReference(userId);
		long monthlyIncome = budgetMonthRepository.findByUser_IdAndBaseDate(userId, baseMonth)
			.map(BudgetMonth::getTotalIncomeAmount)
			.orElse(0L);

		List<EssentialExpenseMonth> essentials = essentialExpenseMonthRepository
			.findAllByBudgetMonth_User_IdOrderByBudgetMonth_BaseDateAscIdAsc(userId);
		List<Plan> activePlans = planRepository.findAllByUser_IdAndStatus(userId, PlanStatus.ACTIVE);

		allocateBudgetUseCase.execute(userReference, baseMonth, monthlyIncome, essentials, activePlans);
	}

	private User userReference(Integer userId) {
		User user = User.createPendingSocialUser();
		user.setId(userId);
		return user;
	}
}

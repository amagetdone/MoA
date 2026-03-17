package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.LocalDate;
import java.util.List;

import com.ssafy.amagetdone.domain.budget.application.usecase.AllocateBudgetUseCase;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseMonth;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetMonthRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseMonthRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BudgetAllocationTriggerService {

	private final AllocateBudgetUseCase allocateBudgetUseCase;
	private final BudgetMonthRepository budgetMonthRepository;
	private final EssentialExpenseMonthRepository essentialExpenseMonthRepository;
	private final PlanRepository planRepository;
	private final EntityManager entityManager;

	public void trigger(Integer userId) {
		LocalDate baseMonth = LocalDate.now().withDayOfMonth(1);
		User userReference = entityManager.getReference(User.class, userId);
		long monthlyIncome = budgetMonthRepository.findByUserAndBaseDate(userReference, baseMonth)
			.map(BudgetMonth::getTotalIncomeAmount)
			.orElse(0L);

		List<EssentialExpenseMonth> essentials = essentialExpenseMonthRepository.findAll().stream()
			.filter(essential -> essential.getBudgetMonth() != null)
			.filter(essential -> essential.getBudgetMonth().getUser() != null)
			.filter(essential -> userId.equals(essential.getBudgetMonth().getUser().getId()))
			.toList();

		List<Plan> activePlans = planRepository.findAll().stream()
			.filter(plan -> plan.getUser() != null)
			.filter(plan -> userId.equals(plan.getUser().getId()))
			.filter(plan -> plan.getStatus() == PlanStatus.ACTIVE)
			.toList();

		allocateBudgetUseCase.execute(userReference, baseMonth, monthlyIncome, essentials, activePlans);
	}
}

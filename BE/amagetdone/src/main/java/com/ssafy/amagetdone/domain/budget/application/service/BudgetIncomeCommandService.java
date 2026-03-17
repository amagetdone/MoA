package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.LocalDate;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetIncomeDto;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetMonth;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetMonthRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BudgetIncomeCommandService {

	private final BudgetMonthRepository budgetMonthRepository;
	private final EntityManager entityManager;

	public BudgetIncomeDto.Response saveIncome(Integer userId, BudgetIncomeDto.Request request) {
		LocalDate baseMonth = LocalDate.now().withDayOfMonth(1);
		User userReference = entityManager.getReference(User.class, userId);
		BudgetMonth budgetMonth = budgetMonthRepository.findByUserAndBaseDate(userReference, baseMonth)
			.orElseGet(() -> BudgetMonth.builder()
				.user(userReference)
				.baseDate(baseMonth)
				.build());

		budgetMonth.setUser(userReference);
		budgetMonth.setBaseDate(baseMonth);
		budgetMonth.setTotalIncomeAmount(request.monthlyIncome());
		BudgetMonth savedBudgetMonth = budgetMonthRepository.save(budgetMonth);

		return new BudgetIncomeDto.Response(savedBudgetMonth.getTotalIncomeAmount(), savedBudgetMonth.getBaseDate());
	}
}

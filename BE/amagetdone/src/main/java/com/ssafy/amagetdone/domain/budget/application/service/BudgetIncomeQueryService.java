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
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BudgetIncomeQueryService {

	private final BudgetMonthRepository budgetMonthRepository;
	private final EntityManager entityManager;

	public BudgetIncomeDto.Response getCurrentIncome(Integer userId) {
		LocalDate baseMonth = LocalDate.now().withDayOfMonth(1);
		User userReference = entityManager.getReference(User.class, userId);
		BudgetIncomeDto.Response response = budgetMonthRepository.findByUserAndBaseDate(userReference, baseMonth)
			.map(budgetMonth -> new BudgetIncomeDto.Response(budgetMonth.getTotalIncomeAmount(), budgetMonth.getBaseDate()))
			.orElseGet(() -> new BudgetIncomeDto.Response(0L, baseMonth));

		return response;
	}
}

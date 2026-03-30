package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.LocalDate;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetIncomeDto;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetMonthRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BudgetIncomeQueryService {

	private final BudgetMonthRepository budgetMonthRepository;

	public BudgetIncomeDto.Response getCurrentIncome(Integer userId) {
		LocalDate baseMonth = LocalDate.now().withDayOfMonth(1);
		BudgetIncomeDto.Response response = budgetMonthRepository.findFirstByUser_IdAndBaseDateOrderByIdDesc(userId, baseMonth)
			.map(budgetMonth -> new BudgetIncomeDto.Response(budgetMonth.getTotalIncomeAmount(), budgetMonth.getBaseDate()))
			.orElseGet(() -> new BudgetIncomeDto.Response(0L, baseMonth));

		return response;
	}
}

package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetDashboardDto;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BudgetDashboardQueryService {

	private final BudgetWeekRepository budgetWeekRepository;

	public BudgetDashboardDto.Response getDashboard(Integer userId) {
		LocalDate today = LocalDate.now();
		BudgetWeek budgetWeek = budgetWeekRepository
			.findFirstByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualOrderByIdDesc(userId, today, today)
			.orElseThrow(() -> new IllegalArgumentException("Current budget week not found"));

		long finalFreeAmount = budgetWeek.getFinalFreeAmount();
		long actualSpentAmount = budgetWeek.getActualSpentAmount();
		long remainingAmount = finalFreeAmount - actualSpentAmount;
		long daysRemaining = Math.max(0L, ChronoUnit.DAYS.between(today, budgetWeek.getWeekEndDate()) + 1);
		long dailyAvailable = daysRemaining > 0 ? remainingAmount / daysRemaining : 0L;

		return new BudgetDashboardDto.Response(
			budgetWeek.getWeekStartDate(),
			budgetWeek.getWeekEndDate(),
			daysRemaining,
			finalFreeAmount,
			actualSpentAmount,
			remainingAmount,
			dailyAvailable
		);
	}
}

package com.ssafy.amagetdone.domain.budget.application.usecase;

import java.time.LocalDate;

import com.ssafy.amagetdone.domain.budget.application.service.EssentialExpenseMonthRefreshService;

import org.springframework.stereotype.Component;

@Component
public class RefreshEssentialExpenseMonthFromAnalysisUseCase {

	private final EssentialExpenseMonthRefreshService essentialExpenseMonthRefreshService;

	public RefreshEssentialExpenseMonthFromAnalysisUseCase(
		EssentialExpenseMonthRefreshService essentialExpenseMonthRefreshService
	) {
		this.essentialExpenseMonthRefreshService = essentialExpenseMonthRefreshService;
	}

	public void execute(Integer userId, LocalDate baseMonth) {
		essentialExpenseMonthRefreshService.refresh(userId, baseMonth);
	}
}

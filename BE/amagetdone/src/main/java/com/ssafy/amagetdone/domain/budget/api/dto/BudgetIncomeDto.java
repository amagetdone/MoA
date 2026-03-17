package com.ssafy.amagetdone.domain.budget.api.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

public final class BudgetIncomeDto {

	private BudgetIncomeDto() {
	}

	@Schema(name = "BudgetIncomeSaveRequest")
	public record Request(
		long monthlyIncome
	) {
	}

	@Schema(name = "BudgetIncomeResponse")
	public record Response(
		long monthlyIncome,
		LocalDate baseMonth
	) {
	}
}

package com.ssafy.amagetdone.domain.transaction.api.dto;

import java.time.LocalDateTime;

public final class MonthlyExpenseDto {

	private MonthlyExpenseDto() {
	}

	public record Response(
		String month,
		Long totalExpenseAmount,
		Long fixedExpenseAmount,
		Long variableExpenseAmount,
		Long excludedIncomeAmount,
		Long excludedInternalTransferAmount,
		Integer transactionCount,
		LocalDateTime lastAnalyzedAt
	) {
	}
}

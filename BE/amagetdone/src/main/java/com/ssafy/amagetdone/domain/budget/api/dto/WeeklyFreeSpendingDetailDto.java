package com.ssafy.amagetdone.domain.budget.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class WeeklyFreeSpendingDetailDto {

	private WeeklyFreeSpendingDetailDto() {
	}

	public record Response(
		String yearWeek,
		LocalDate weekStartDate,
		LocalDate weekEndDate,
		Long allocatedAmount,
		Long spentAmount,
		Long remainingAmount,
		List<TransactionDetail> transactions
	) {
	}

	public record TransactionDetail(
		Integer transactionId,
		String sourceType,
		String sourceId,
		LocalDateTime occurredAt,
		String merchantText,
		Long amount,
		CategoryView category,
		String expenseType,
		boolean analysisIncluded,
		String excludedReason
	) {
	}

	public record CategoryView(
		Integer id,
		String name
	) {
	}
}

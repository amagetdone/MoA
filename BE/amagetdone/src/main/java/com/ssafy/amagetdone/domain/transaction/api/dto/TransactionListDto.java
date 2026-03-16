package com.ssafy.amagetdone.domain.transaction.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class TransactionListDto {

	private TransactionListDto() {
	}

	public record Query(
		LocalDate fromDate,
		LocalDate toDate,
		String analysisInclusion,
		Integer page,
		Integer size
	) {
	}

	public record Response(
		Sync sync,
		Integer page,
		Integer size,
		Long totalElements,
		List<Item> items
	) {
	}

	public record Sync(
		String status,
		boolean triggered,
		LocalDateTime syncedAt
	) {
	}

	public record Item(
		Integer transactionId,
		LocalDateTime occurredAt,
		String merchantText,
		Long amount,
		String direction,
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

package com.ssafy.amagetdone.domain.transaction.infra.client.dto;

import java.util.List;

public final class DemandDepositTransactionDto {

	private DemandDepositTransactionDto() {
	}

	public record Request(
		String userKey,
		String accountNo,
		String startDate,
		String endDate,
		String transactionType,
		String orderByType
	) {
	}

	public record Response(
		Integer totalCount,
		List<Item> transactions
	) {
	}

	public record Item(
		Long transactionUniqueNo,
		String transactionDate,
		String transactionTime,
		String transactionType,
		String transactionTypeName,
		String transactionAccountNo,
		Long transactionBalance,
		Long transactionAfterBalance,
		String transactionSummary,
		String transactionMemo
	) {
	}

	public record ApiRequest(
		String accountNo,
		String startDate,
		String endDate,
		String transactionType,
		String orderByType
	) {
	}

	public record ApiResponse(
		Integer totalCount,
		List<ApiItem> list
	) {
	}

	public record ApiItem(
		Long transactionUniqueNo,
		String transactionDate,
		String transactionTime,
		String transactionType,
		String transactionTypeName,
		String transactionAccountNo,
		Long transactionBalance,
		Long transactionAfterBalance,
		String transactionSummary,
		String transactionMemo
	) {
	}
}

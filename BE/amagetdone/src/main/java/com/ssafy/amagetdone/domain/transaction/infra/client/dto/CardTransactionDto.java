package com.ssafy.amagetdone.domain.transaction.infra.client.dto;

import java.util.List;

public final class CardTransactionDto {

	private CardTransactionDto() {
	}

	public record Request(
		String userKey,
		String cardNo,
		String cvc,
		String startDate,
		String endDate
	) {
	}

	public record Response(
		String cardIssuerCode,
		String cardIssuerName,
		String cardName,
		String cardNo,
		Long estimatedBalance,
		List<Item> transactions
	) {
	}

	public record Item(
		Long transactionUniqueNo,
		String categoryId,
		String categoryName,
		Long merchantId,
		String merchantName,
		String transactionDate,
		String transactionTime,
		Long transactionBalance,
		String cardStatus,
		String billStatementsYn,
		String billStatementsStatus
	) {
	}

	public record ApiRequest(
		String cardNo,
		String cvc,
		String startDate,
		String endDate
	) {
	}

	public record ApiResponse(
		String cardIssuerCode,
		String cardIssuerName,
		String cardName,
		String cardNo,
		Long estimatedBalance,
		List<ApiItem> transactionList
	) {
	}

	public record ApiItem(
		Long transactionUniqueNo,
		String categoryId,
		String categoryName,
		Long merchantId,
		String merchantName,
		String transactionDate,
		String transactionTime,
		Long transactionBalance,
		String cardStatus,
		String billStatementsYn,
		String billStatementsStatus
	) {
	}
}

package com.ssafy.amagetdone.domain.account.api.dto;

import java.util.List;

public final class AccountDto {

	private AccountDto() {
	}

	public record AccountSummaryResponse(
		String accountNo,
		String bankCode,
		String bankName,
		String accountName,
		String alias,
		boolean verified
	) {
	}

	public record AccountBalanceResponse(
		String accountNo,
		Long accountBalance,
		String accountCreatedDate,
		String accountExpiryDate,
		String lastTransactionDate,
		String currency
	) {
	}

	public record DepositRequest(
		String accountNo,
		Long transactionBalance,
		String transactionSummary
	) {
	}

	public record DepositResponse(
		Long transactionUniqueNo,
		String transactionDate
	) {
	}

	public record TransferRequest(
		String withdrawalAccountNo,
		String depositAccountNo,
		Long transactionBalance,
		String withdrawalTransactionSummary,
		String depositTransactionSummary
	) {
	}

	public record TransferItemResponse(
		Long transactionUniqueNo,
		String accountNo,
		String transactionDate,
		String transactionType,
		String transactionTypeName,
		String transactionAccountNo
	) {
	}

	public record TransferResponse(
		List<TransferItemResponse> transactions
	) {
	}

	public record TransactionHistoryResponse(
		Integer totalCount,
		List<TransactionItemResponse> transactions
	) {
	}

	public record TransactionItemResponse(
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

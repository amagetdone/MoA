package com.ssafy.amagetdone.domain.account.infra.client.dto;

import java.util.List;

public final class DemandDepositTransferDto {

	private DemandDepositTransferDto() {
	}

	public record DepositRequest(
		String userKey,
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
		String userKey,
		String withdrawalAccountNo,
		String depositAccountNo,
		Long transactionBalance,
		String withdrawalTransactionSummary,
		String depositTransactionSummary
	) {
	}

	public record TransferResponse(
		List<TransferItem> transactions
	) {
	}

	public record TransferItem(
		Long transactionUniqueNo,
		String accountNo,
		String transactionDate,
		String transactionType,
		String transactionTypeName,
		String transactionAccountNo
	) {
	}
}

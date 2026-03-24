package com.ssafy.amagetdone.domain.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public final class AccountAdminDto {

	private AccountAdminDto() {
	}

	public record ProductCreateRequest(
		String bankCode,
		String accountName,
		String accountDescription
	) {
	}

	public record ProductResponse(
		String accountTypeUniqueNo,
		String bankCode,
		String bankName,
		String accountTypeCode,
		String accountTypeName,
		String accountName,
		String accountDescription,
		String accountType
	) {
	}

	public record DataInitRequest(
		String apiKey,
		String userApiKey
	) {
	}

	public record AccountCreateRequest(
		Integer userId,
		String accountTypeUniqueNo
	) {
	}

	public record AccountCreateResponse(
		String bankCode,
		String accountNo,
		String currency,
		String currencyName
	) {
	}

	@Schema(name = "AccountTransactionInitRequest")
	public record AccountTransactionInitRequest(
		@Schema(
			description = "Target account number from local Account.accountNo",
			example = "0016174648358792"
		)
		String accountNo
	) {
	}

	@Schema(name = "CardTransactionInitRequest")
	public record CardTransactionInitRequest(
		@Schema(
			description = "Target card number from local Card.cardNo",
			example = "5241111122223333"
		)
		String cardNo
	) {
	}

	@Schema(name = "TransactionInitResponse")
	public record TransactionInitResponse(
		@Schema(description = "Target type", example = "account")
		String targetType,
		@Schema(description = "Target number", example = "0016174648358792")
		String targetNo,
		@Schema(description = "Created transaction count", example = "109")
		int transactionCount
	) {
	}

	@Schema(name = "AccountPaymentRequest")
	public record AccountPaymentRequest(
		@Schema(description = "Withdraw account number", example = "0016174648358792")
		String accountNo,
		@Schema(description = "Deposit account number", example = "0010000000000001")
		String counterpartyAccountNo,
		@Schema(description = "Transfer amount", example = "10000")
		Long amount,
		@Schema(description = "Withdrawal summary", example = "테스트 출금")
		String withdrawalSummary,
		@Schema(description = "Deposit summary", example = "테스트 입금")
		String depositSummary
	) {
	}

	@Schema(name = "CardPaymentRequest")
	public record CardPaymentRequest(
		@Schema(description = "Card number", example = "5241111122223333")
		String cardNo,
		@Schema(description = "Merchant id", example = "101001")
		Long merchantId,
		@Schema(description = "Payment amount", example = "28900")
		Long amount
	) {
	}

	@Schema(name = "CardPaymentCancelRequest")
	public record CardPaymentCancelRequest(
		@Schema(description = "Card number", example = "5241111122223333")
		String cardNo,
		@Schema(description = "Transaction unique number to cancel", example = "88")
		Long transactionUniqueNo
	) {
	}

	@Schema(name = "PaymentResponse")
	public record PaymentResponse(
		@Schema(description = "Target type", example = "card")
		String targetType,
		@Schema(description = "Target number", example = "5241111122223333")
		String targetNo,
		@Schema(description = "Transaction unique number", example = "88")
		Long transactionUniqueNo,
		@Schema(description = "Transaction date", example = "20260324")
		String transactionDate,
		@Schema(description = "Transaction time", example = "092700")
		String transactionTime,
		@Schema(description = "Transaction amount", example = "28900")
		Long amount,
		@Schema(description = "Transaction status", example = "BILLED")
		String status
	) {
	}
}

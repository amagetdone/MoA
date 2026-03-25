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
			description = "우리 시스템에 저장된 계좌번호",
			example = "0016174648358792"
		)
		String accountNo
	) {
	}

	@Schema(name = "CardTransactionInitRequest")
	public record CardTransactionInitRequest(
		@Schema(
			description = "우리 시스템에 저장된 카드번호",
			example = "5241111122223333"
		)
		String cardNo
	) {
	}

	@Schema(name = "TransactionInitResponse")
	public record TransactionInitResponse(
		@Schema(description = "대상 유형", example = "account")
		String targetType,
		@Schema(description = "대상 번호", example = "0016174648358792")
		String targetNo,
		@Schema(description = "생성된 거래 건수", example = "109")
		int transactionCount
	) {
	}

	@Schema(name = "AccountPaymentRequest")
	public record AccountPaymentRequest(
		@Schema(description = "출금 계좌번호", example = "0016174648358792")
		String accountNo,
		@Schema(description = "입금 상대 계좌번호", example = "0010000000000001")
		String counterpartyAccountNo,
		@Schema(description = "이체 금액", example = "10000")
		Long amount,
		@Schema(description = "출금 거래 요약", example = "테스트 출금")
		String withdrawalSummary,
		@Schema(description = "입금 거래 요약", example = "테스트 입금")
		String depositSummary
	) {
	}

	@Schema(name = "CardPaymentRequest")
	public record CardPaymentRequest(
		@Schema(description = "결제 카드번호", example = "5241111122223333")
		String cardNo,
		@Schema(description = "가맹점 ID", example = "101001")
		Long merchantId,
		@Schema(description = "결제 금액", example = "28900")
		Long amount
	) {
	}

	@Schema(name = "CardPaymentCancelRequest")
	public record CardPaymentCancelRequest(
		@Schema(description = "결제 카드번호", example = "5241111122223333")
		String cardNo,
		@Schema(description = "취소할 거래 고유번호", example = "88")
		Long transactionUniqueNo
	) {
	}

	@Schema(name = "PaymentResponse")
	public record PaymentResponse(
		@Schema(description = "대상 유형", example = "card")
		String targetType,
		@Schema(description = "대상 번호", example = "5241111122223333")
		String targetNo,
		@Schema(description = "거래 고유번호", example = "88")
		Long transactionUniqueNo,
		@Schema(description = "거래 일자", example = "20260324")
		String transactionDate,
		@Schema(description = "거래 시각", example = "092700")
		String transactionTime,
		@Schema(description = "거래 금액", example = "28900")
		Long amount,
		@Schema(description = "거래 상태", example = "BILLED")
		String status
	) {
	}
}

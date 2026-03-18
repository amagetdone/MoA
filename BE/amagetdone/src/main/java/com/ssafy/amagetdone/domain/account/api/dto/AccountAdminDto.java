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
			description = "거래내역을 적재할 대상 계좌번호. 우리 DB의 Account.accountNo 값이며 AccountUniqueNo가 아니라 실제 계좌번호를 입력한다.",
			example = "0016174648358792"
		)
		String accountNo
	) {
	}

	@Schema(name = "CardTransactionInitRequest")
	public record CardTransactionInitRequest(
		@Schema(
			description = "거래내역을 적재할 대상 카드번호. 우리 DB의 Card.cardNo 값이며 cardUniqueNo가 아니라 카드 앞면 기준 카드번호를 입력한다.",
			example = "5241111122223333"
		)
		String cardNo
	) {
	}

	@Schema(name = "TransactionInitResponse")
	public record TransactionInitResponse(
		@Schema(description = "적재 대상 타입", example = "account")
		String targetType,
		@Schema(description = "적재 대상 번호", example = "0016174648358792")
		String targetNo,
		@Schema(description = "하드코딩 시드의 기준 월", example = "2025-12")
		String baseMonth,
		@Schema(description = "적재된 거래내역 건수", example = "109")
		int transactionCount
	) {
	}
}

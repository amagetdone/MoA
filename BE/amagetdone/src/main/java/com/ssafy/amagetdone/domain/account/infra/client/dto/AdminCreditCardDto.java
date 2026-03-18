package com.ssafy.amagetdone.domain.account.infra.client.dto;

public final class AdminCreditCardDto {

	private AdminCreditCardDto() {
	}

	public record CreatedCard(
		String cardNo,
		String cvc,
		String cardUniqueNo,
		String cardIssuerCode,
		String cardIssuerName,
		String cardName,
		Long baselinePerformance,
		Long maxBenefitLimit,
		String cardDescription,
		String cardExpiryDate,
		String withdrawalAccountNo,
		String withdrawalDate
	) {
	}
}

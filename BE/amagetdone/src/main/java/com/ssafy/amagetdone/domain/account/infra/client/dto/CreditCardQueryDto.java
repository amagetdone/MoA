package com.ssafy.amagetdone.domain.account.infra.client.dto;

import java.util.List;

public final class CreditCardQueryDto {

	private CreditCardQueryDto() {
	}

	public record Request(
		String userKey
	) {
	}

	public record Response(
		List<CardItem> cards
	) {
	}

	public record CardItem(
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

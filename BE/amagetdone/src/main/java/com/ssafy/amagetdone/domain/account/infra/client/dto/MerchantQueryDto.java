package com.ssafy.amagetdone.domain.account.infra.client.dto;

import java.util.List;

public final class MerchantQueryDto {

	private MerchantQueryDto() {
	}

	public record Response(
		List<MerchantItem> merchants
	) {
	}

	public record MerchantItem(
		String categoryId,
		String categoryName,
		Long merchantId,
		String merchantName
	) {
	}
}

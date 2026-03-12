package com.ssafy.amagetdone.domain.asset.infra.client;

public record MerchantItem(
	String categoryId,
	String categoryName,
	Long merchantId,
	String merchantName
) {
}

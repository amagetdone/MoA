package com.ssafy.amagetdone.domain.asset.infra.client;

import java.util.List;

public record MerchantListResult(
	List<MerchantItem> merchants
) {
}

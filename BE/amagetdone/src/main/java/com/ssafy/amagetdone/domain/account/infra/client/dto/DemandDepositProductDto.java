package com.ssafy.amagetdone.domain.account.infra.client.dto;

import java.util.List;

public final class DemandDepositProductDto {

	private DemandDepositProductDto() {
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

	public record ApiProductResponse(
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

	public record ProductListResponse(
		List<ProductResponse> products
	) {
	}
}

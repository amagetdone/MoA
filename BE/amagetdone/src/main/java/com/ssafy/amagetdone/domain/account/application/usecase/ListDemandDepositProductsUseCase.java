package com.ssafy.amagetdone.domain.account.application.usecase;

import java.util.List;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.infra.client.SsafyDemandDepositProductApiClient;

import org.springframework.stereotype.Component;

@Component
public class ListDemandDepositProductsUseCase {

	private final SsafyDemandDepositProductApiClient productApiClient;

	public ListDemandDepositProductsUseCase(SsafyDemandDepositProductApiClient productApiClient) {
		this.productApiClient = productApiClient;
	}

	public List<AccountAdminDto.ProductResponse> execute() {
		return productApiClient.inquireProducts().stream()
			.map(product -> new AccountAdminDto.ProductResponse(
				product.accountTypeUniqueNo(),
				product.bankCode(),
				product.bankName(),
				product.accountTypeCode(),
				product.accountTypeName(),
				product.accountName(),
				product.accountDescription(),
				product.accountType()
			))
			.toList();
	}
}

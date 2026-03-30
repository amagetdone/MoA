package com.ssafy.amagetdone.domain.account.application.usecase;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.infra.client.SsafyDemandDepositProductApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositProductDto;

import org.springframework.stereotype.Component;

@Component
public class RegisterDemandDepositProductUseCase {

	private final SsafyDemandDepositProductApiClient productApiClient;

	public RegisterDemandDepositProductUseCase(SsafyDemandDepositProductApiClient productApiClient) {
		this.productApiClient = productApiClient;
	}

	public AccountAdminDto.ProductResponse execute(AccountAdminDto.ProductCreateRequest request) {
		DemandDepositProductDto.ProductResponse response = productApiClient.createProduct(
			new DemandDepositProductDto.ProductCreateRequest(
				request.bankCode(),
				request.accountName(),
				request.accountDescription()
			)
		);
		return new AccountAdminDto.ProductResponse(
			response.accountTypeUniqueNo(),
			response.bankCode(),
			response.bankName(),
			response.accountTypeCode(),
			response.accountTypeName(),
			response.accountName(),
			response.accountDescription(),
			response.accountType()
		);
	}
}

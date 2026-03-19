package com.ssafy.amagetdone.domain.account.api.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.application.service.AccountService;
import com.ssafy.amagetdone.global.response.ApiResponse;

import org.junit.jupiter.api.Test;

class AccountAdminControllerTest {

	@Test
	void registerProductReturnsWrappedResponse() {
		AccountService accountService = mock(AccountService.class);
		AccountAdminController controller = new AccountAdminController(accountService);
		AccountAdminDto.ProductCreateRequest request = new AccountAdminDto.ProductCreateRequest("001", "데모 상품", "설명");
		AccountAdminDto.ProductResponse response = new AccountAdminDto.ProductResponse(
			"001-1-abc",
			"001",
			"한국은행",
			"1",
			"수시입출금",
			"데모 상품",
			"설명",
			"DOMESTIC"
		);
		when(accountService.registerProduct(request)).thenReturn(response);

		ApiResponse<AccountAdminDto.ProductResponse> result = controller.registerProduct(request);

		assertThat(result.status()).isEqualTo("ok");
		assertThat(result.data()).isEqualTo(response);
		verify(accountService).registerProduct(request);
	}
}

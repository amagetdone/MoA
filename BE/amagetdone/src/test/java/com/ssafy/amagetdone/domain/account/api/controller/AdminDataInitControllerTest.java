package com.ssafy.amagetdone.domain.account.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.application.service.AccountService;
import com.ssafy.amagetdone.global.response.ApiResponse;

import org.junit.jupiter.api.Test;

class AdminDataInitControllerTest {

	@Test
	void initializeDummyDataReturnsWrappedResponse() {
		AccountService accountService = mock(AccountService.class);
		AdminDataInitController controller = new AdminDataInitController(accountService);
		AccountAdminDto.DataInitRequest request = new AccountAdminDto.DataInitRequest("admin-api-key", "user-api-key");
		AccountAdminDto.DataInitResponse response = new AccountAdminDto.DataInitResponse(
			"0016174648358792",
			"1003622654847049",
			"1001-eebb34baf8204cf",
			4,
			4
		);
		when(accountService.initializeDummyData(request)).thenReturn(response);

		ApiResponse<AccountAdminDto.DataInitResponse> result = controller.initialize(request);

		assertThat(result.status()).isEqualTo("ok");
		assertThat(result.data()).isEqualTo(response);
		verify(accountService).initializeDummyData(request);
	}
}

package com.ssafy.amagetdone.domain.account.api.controller;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.application.service.AccountService;
import com.ssafy.amagetdone.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Data Init API")
@RestController
@RequestMapping("/admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminDataInitController {

	private final AccountService accountService;

	public AdminDataInitController(AccountService accountService) {
		this.accountService = accountService;
	}

	@Operation(
		summary = "Initialize account transaction overrides",
		description = "request.accountNo에는 우리 DB의 Account.accountNo 값을 넣는다. AccountUniqueNo가 아니라 실제 계좌번호를 입력해야 한다."
	)
	@PostMapping("/data-init/account")
	public ApiResponse<AccountAdminDto.TransactionInitResponse> initializeAccountTransactions(
		@RequestBody AccountAdminDto.AccountTransactionInitRequest request
	) {
		return ApiResponse.ok(
			"Account transactions initialized.",
			accountService.initializeAccountTransactions(request)
		);
	}

	@Operation(
		summary = "Initialize card transaction overrides",
		description = "request.cardNo에는 우리 DB의 Card.cardNo 값을 넣는다. cardUniqueNo가 아니라 카드 앞면 기준 카드번호를 입력해야 한다."
	)
	@PostMapping("/data-init/card")
	public ApiResponse<AccountAdminDto.TransactionInitResponse> initializeCardTransactions(
		@RequestBody AccountAdminDto.CardTransactionInitRequest request
	) {
		return ApiResponse.ok(
			"Card transactions initialized.",
			accountService.initializeCardTransactions(request)
		);
	}
}

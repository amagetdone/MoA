package com.ssafy.amagetdone.domain.accountverification.api.controller;

import com.ssafy.amagetdone.domain.accountverification.api.dto.AccountVerificationAdminDto;
import com.ssafy.amagetdone.domain.accountverification.application.service.AccountVerificationService;
import com.ssafy.amagetdone.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "계좌 인증 관리자 API")
@RestController
@RequestMapping("/v1/admin/accounts/verification")
public class AccountVerificationAdminController {

	private final AccountVerificationService accountVerificationService;

	public AccountVerificationAdminController(AccountVerificationService accountVerificationService) {
		this.accountVerificationService = accountVerificationService;
	}

	@Operation(summary = "1원 인증 검증")
	@PostMapping("/check")
	public ApiResponse<AccountVerificationAdminDto.CheckVerificationResponse> checkVerification(
		@RequestBody AccountVerificationAdminDto.CheckVerificationRequest request
	) {
		return ApiResponse.ok("1원 인증 검증을 완료했습니다.", accountVerificationService.checkVerification(request));
	}

	@Operation(summary = "1원 인증 상태 조회")
	@GetMapping("/{accountNo}")
	public ApiResponse<AccountVerificationAdminDto.VerificationStatusResponse> getVerificationStatus(
		@PathVariable String accountNo,
		@RequestParam Integer userId
	) {
		return ApiResponse.ok(
			"1원 인증 상태를 조회했습니다.",
			accountVerificationService.getVerificationStatus(userId, accountNo)
		);
	}
}

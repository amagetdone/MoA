package com.ssafy.amagetdone.domain.accountverification.api.controller;

import com.ssafy.amagetdone.domain.accountverification.api.dto.AccountVerificationDto;
import com.ssafy.amagetdone.domain.accountverification.application.service.AccountVerificationService;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "계좌 인증 API")
@RestController
@RequestMapping("/v1/accounts/verification")
@SecurityRequirement(name = "bearerAuth")
public class AccountVerificationController {

	private final AccountVerificationService accountVerificationService;
	private final JwtAccessTokenReader jwtAccessTokenReader;

	public AccountVerificationController(
		AccountVerificationService accountVerificationService,
		JwtAccessTokenReader jwtAccessTokenReader
	) {
		this.accountVerificationService = accountVerificationService;
		this.jwtAccessTokenReader = jwtAccessTokenReader;
	}

	@Operation(summary = "1원 인증 요청")
	@PostMapping("/requests")
	public ApiResponse<AccountVerificationDto.RequestVerificationResponse> requestVerification(
		@Parameter(hidden = true)
		@RequestHeader(name = "Authorization", required = false) String authorization,
		@RequestBody AccountVerificationDto.RequestVerificationRequest request
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		return ApiResponse.ok(
			"1원 인증 요청을 생성했습니다.",
			accountVerificationService.requestVerification(userId, request)
		);
	}

	@Operation(summary = "1원 인증 코드 확인")
	@PostMapping("/check")
	public ApiResponse<AccountVerificationDto.CheckVerificationResponse> checkVerification(
		@Parameter(hidden = true)
		@RequestHeader(name = "Authorization", required = false) String authorization,
		@RequestBody AccountVerificationDto.CheckVerificationRequest request
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		return ApiResponse.ok(
			"1원 인증을 완료했습니다.",
			accountVerificationService.checkVerification(userId, request)
		);
	}

	@Operation(summary = "1원 인증 상태 조회")
	@GetMapping("/{accountNo}")
	public ApiResponse<AccountVerificationDto.VerificationStatusResponse> getVerificationStatus(
		@Parameter(hidden = true)
		@RequestHeader(name = "Authorization", required = false) String authorization,
		@PathVariable String accountNo
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		return ApiResponse.ok(
			"1원 인증 상태를 조회했습니다.",
			accountVerificationService.getVerificationStatus(userId, accountNo)
		);
	}
}

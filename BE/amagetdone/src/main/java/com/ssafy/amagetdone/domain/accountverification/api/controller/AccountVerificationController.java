package com.ssafy.amagetdone.domain.accountverification.api.controller;

import com.ssafy.amagetdone.domain.accountverification.api.dto.AccountVerificationDto;
import com.ssafy.amagetdone.domain.accountverification.application.service.AccountVerificationService;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Account Verification User API")
@RestController
@RequestMapping("/v1/accounts/verification")
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
		@RequestHeader(name = "Authorization", required = false) String authorization,
		@RequestBody AccountVerificationDto.RequestVerificationRequest request
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		return ApiResponse.ok(
			"1원 인증 요청이 생성되었습니다.",
			accountVerificationService.requestVerification(userId, request)
		);
	}
}

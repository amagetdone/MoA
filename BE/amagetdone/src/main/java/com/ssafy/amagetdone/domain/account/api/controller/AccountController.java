// Responsibility: Exposes authenticated account endpoints for linked-account user actions.
package com.ssafy.amagetdone.domain.account.api.controller;

import java.util.List;

import com.ssafy.amagetdone.domain.account.api.dto.AccountDto;
import com.ssafy.amagetdone.domain.account.application.service.AccountService;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "계좌 사용자 API")
@RestController
@RequestMapping("/v1/accounts")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

	private final AccountService accountService;
	private final JwtAccessTokenReader jwtAccessTokenReader;

	public AccountController(AccountService accountService, JwtAccessTokenReader jwtAccessTokenReader) {
		this.accountService = accountService;
		this.jwtAccessTokenReader = jwtAccessTokenReader;
	}

	@Operation(summary = "Ssafy 은행 계좌 목록 조회 (연동 전)")
	@GetMapping("/bank")
	public ApiResponse<List<AccountDto.BankAccountResponse>> getBankAccounts(
		@Parameter(hidden = true)
		@RequestHeader(name = "Authorization", required = false) String authorization
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		return ApiResponse.ok("은행 계좌 목록을 조회했습니다.", accountService.getBankAccounts(userId));
	}

	@Operation(summary = "계좌 실소유 확인")
	@PostMapping("/confirm")
	public ApiResponse<AccountDto.ConfirmAccountResponse> confirmAccount(
		@Parameter(hidden = true)
		@RequestHeader(name = "Authorization", required = false) String authorization,
		@RequestBody AccountDto.ConfirmAccountRequest request
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		return ApiResponse.ok("계좌 실소유를 확인했습니다.", accountService.confirmAccount(userId, request));
	}

	@Operation(summary = "단일 계좌 조회")
	@GetMapping("/{accountNo}")
	public ApiResponse<AccountDto.AccountSummaryResponse> getAccount(
		@Parameter(hidden = true)
		@RequestHeader(name = "Authorization", required = false) String authorization,
		@PathVariable String accountNo
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		return ApiResponse.ok("계좌를 조회했습니다.", accountService.getAccount(userId, accountNo));
	}

	@Operation(summary = "내 계좌 목록 조회")
	@GetMapping("/me")
	public ApiResponse<List<AccountDto.AccountSummaryResponse>> getAccounts(
		@Parameter(hidden = true)
		@RequestHeader(name = "Authorization", required = false) String authorization
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		return ApiResponse.ok("내 계좌 목록을 조회했습니다.", accountService.getAccounts(userId));
	}

	@Operation(summary = "연결 계좌 삭제")
	@DeleteMapping("/{accountNo}")
	public ApiResponse<Void> deleteAccount(
		@Parameter(hidden = true)
		@RequestHeader(name = "Authorization", required = false) String authorization,
		@PathVariable String accountNo
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		accountService.deleteAccount(userId, accountNo);
		return ApiResponse.ok("계좌 연결이 해제되었습니다.", null);
	}
}

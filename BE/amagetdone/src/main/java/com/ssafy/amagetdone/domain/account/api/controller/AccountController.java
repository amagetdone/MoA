package com.ssafy.amagetdone.domain.account.api.controller;

import java.util.List;

import com.ssafy.amagetdone.domain.account.api.dto.AccountDto;
import com.ssafy.amagetdone.domain.account.application.service.AccountService;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Account User API")
@RestController
@RequestMapping("/v1/accounts")
public class AccountController {

	private final AccountService accountService;
	private final JwtAccessTokenReader jwtAccessTokenReader;

	public AccountController(AccountService accountService, JwtAccessTokenReader jwtAccessTokenReader) {
		this.accountService = accountService;
		this.jwtAccessTokenReader = jwtAccessTokenReader;
	}

	@Operation(summary = "내 계좌 연동 동기화")
	@PostMapping("/me/sync")
	public ApiResponse<List<AccountDto.AccountSummaryResponse>> syncAccounts(
		@RequestHeader(name = "Authorization", required = false) String authorization
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		return ApiResponse.ok("내 계좌 목록을 동기화했습니다.", accountService.syncAccounts(userId));
	}

	@Operation(summary = "내 계좌 목록 조회")
	@GetMapping("/me")
	public ApiResponse<List<AccountDto.AccountSummaryResponse>> getAccounts(
		@RequestHeader(name = "Authorization", required = false) String authorization
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		return ApiResponse.ok("내 계좌 목록을 조회했습니다.", accountService.getAccounts(userId));
	}

	@Operation(summary = "계좌 잔액 조회")
	@GetMapping("/{accountNo}/balance")
	public ApiResponse<AccountDto.AccountBalanceResponse> getBalance(
		@RequestHeader(name = "Authorization", required = false) String authorization,
		@PathVariable String accountNo
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		return ApiResponse.ok("계좌 잔액을 조회했습니다.", accountService.getBalance(userId, accountNo));
	}

	@Operation(summary = "계좌 입금")
	@PostMapping("/deposit")
	public ApiResponse<AccountDto.DepositResponse> deposit(
		@RequestHeader(name = "Authorization", required = false) String authorization,
		@RequestBody AccountDto.DepositRequest request
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		return ApiResponse.ok("계좌 입금이 완료되었습니다.", accountService.deposit(userId, request));
	}

	@Operation(summary = "계좌 이체")
	@PostMapping("/transfer")
	public ApiResponse<AccountDto.TransferResponse> transfer(
		@RequestHeader(name = "Authorization", required = false) String authorization,
		@RequestBody AccountDto.TransferRequest request
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		return ApiResponse.ok("계좌 이체가 완료되었습니다.", accountService.transfer(userId, request));
	}

	@Operation(summary = "계좌 거래내역 목록 조회")
	@GetMapping("/{accountNo}/transactions")
	public ApiResponse<AccountDto.TransactionHistoryResponse> getTransactions(
		@RequestHeader(name = "Authorization", required = false) String authorization,
		@PathVariable String accountNo,
		@RequestParam String startDate,
		@RequestParam String endDate,
		@RequestParam(defaultValue = "A") String transactionType,
		@RequestParam(defaultValue = "ASC") String orderByType
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		return ApiResponse.ok(
			"계좌 거래내역을 조회했습니다.",
			accountService.getTransactions(userId, accountNo, startDate, endDate, transactionType, orderByType)
		);
	}
}

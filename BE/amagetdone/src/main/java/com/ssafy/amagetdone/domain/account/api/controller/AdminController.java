package com.ssafy.amagetdone.domain.account.api.controller;

import java.util.List;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.application.service.AccountAdminService;
import com.ssafy.amagetdone.domain.account.application.service.AdminPaymentService;
import com.ssafy.amagetdone.global.response.ApiResponse;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 API")
@RestController
@RequestMapping("/admin")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminController {

	private final AccountAdminService accountAdminService;
	private final AdminPaymentService adminPaymentService;

	@Operation(summary = "수시입출금 상품 등록")
	@PostMapping("/accounts/products")
	public ApiResponse<AccountAdminDto.ProductResponse> registerProduct(@RequestBody AccountAdminDto.ProductCreateRequest request) {
		return ApiResponse.ok("수시입출금 상품을 등록했습니다.", accountAdminService.registerProduct(request));
	}

	@Operation(summary = "수시입출금 상품 목록 조회")
	@GetMapping("/accounts/products")
	public ApiResponse<List<AccountAdminDto.ProductResponse>> getProducts() {
		return ApiResponse.ok("수시입출금 상품 목록을 조회했습니다.", accountAdminService.listProducts());
	}

	@Operation(summary = "계좌 거래내역 초기화")
	@PostMapping("/data-init/account")
	public ApiResponse<AccountAdminDto.TransactionInitResponse> initializeAccountTransactions(
		@RequestBody AccountAdminDto.AccountTransactionInitRequest request
	) {
		return ApiResponse.ok(
			"계좌 거래내역을 초기화했습니다.",
			accountAdminService.initializeAccountTransactions(request)
		);
	}

	@Operation(summary = "카드 거래내역 초기화")
	@PostMapping("/data-init/card")
	public ApiResponse<AccountAdminDto.TransactionInitResponse> initializeCardTransactions(
		@RequestBody AccountAdminDto.CardTransactionInitRequest request
	) {
		return ApiResponse.ok(
			"카드 거래내역을 초기화했습니다.",
			accountAdminService.initializeCardTransactions(request)
		);
	}

	@Operation(summary = "관리자 계좌 이체 결제 실행")
	@PostMapping("/payments/account")
	public ApiResponse<AccountAdminDto.PaymentResponse> executeAccountPayment(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@RequestBody AccountAdminDto.AccountPaymentRequest request
	) {
		return ApiResponse.ok(
			"계좌 이체 결제를 완료했습니다.",
			adminPaymentService.executeAccountPayment(authenticatedMember.userId(), request)
		);
	}

	@Operation(summary = "관리자 카드 결제 실행")
	@PostMapping("/payments/card")
	public ApiResponse<AccountAdminDto.PaymentResponse> executeCardPayment(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@RequestBody AccountAdminDto.CardPaymentRequest request
	) {
		return ApiResponse.ok(
			"카드 결제를 완료했습니다.",
			adminPaymentService.executeCardPayment(authenticatedMember.userId(), request)
		);
	}

	@Operation(summary = "관리자 카드 결제 취소")
	@PostMapping("/payments/card/cancel")
	public ApiResponse<AccountAdminDto.PaymentResponse> cancelCardPayment(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@RequestBody AccountAdminDto.CardPaymentCancelRequest request
	) {
		return ApiResponse.ok(
			"카드 결제를 취소했습니다.",
			adminPaymentService.cancelCardPayment(authenticatedMember.userId(), request)
		);
	}
}

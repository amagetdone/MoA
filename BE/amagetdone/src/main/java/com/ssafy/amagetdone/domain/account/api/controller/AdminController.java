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

@Tag(name = "Admin API")
@RestController
@RequestMapping("/admin")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminController {

	private final AccountAdminService accountAdminService;
	private final AdminPaymentService adminPaymentService;

	@Operation(summary = "Register demand deposit product")
	@PostMapping("/accounts/products")
	public ApiResponse<AccountAdminDto.ProductResponse> registerProduct(@RequestBody AccountAdminDto.ProductCreateRequest request) {
		return ApiResponse.ok("Demand deposit product registered.", accountAdminService.registerProduct(request));
	}

	@Operation(summary = "List demand deposit products")
	@GetMapping("/accounts/products")
	public ApiResponse<List<AccountAdminDto.ProductResponse>> getProducts() {
		return ApiResponse.ok("Demand deposit products retrieved.", accountAdminService.listProducts());
	}

	@Operation(summary = "Initialize account transaction overrides")
	@PostMapping("/data-init/account")
	public ApiResponse<AccountAdminDto.TransactionInitResponse> initializeAccountTransactions(
		@RequestBody AccountAdminDto.AccountTransactionInitRequest request
	) {
		return ApiResponse.ok(
			"Account transactions initialized.",
			accountAdminService.initializeAccountTransactions(request)
		);
	}

	@Operation(summary = "Initialize card transaction overrides")
	@PostMapping("/data-init/card")
	public ApiResponse<AccountAdminDto.TransactionInitResponse> initializeCardTransactions(
		@RequestBody AccountAdminDto.CardTransactionInitRequest request
	) {
		return ApiResponse.ok(
			"Card transactions initialized.",
			accountAdminService.initializeCardTransactions(request)
		);
	}

	@Operation(summary = "Execute admin account payment")
	@PostMapping("/payments/account")
	public ApiResponse<AccountAdminDto.PaymentResponse> executeAccountPayment(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@RequestBody AccountAdminDto.AccountPaymentRequest request
	) {
		return ApiResponse.ok(
			"Account payment completed.",
			adminPaymentService.executeAccountPayment(authenticatedMember.userId(), request)
		);
	}

	@Operation(summary = "Execute admin card payment")
	@PostMapping("/payments/card")
	public ApiResponse<AccountAdminDto.PaymentResponse> executeCardPayment(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@RequestBody AccountAdminDto.CardPaymentRequest request
	) {
		return ApiResponse.ok(
			"Card payment completed.",
			adminPaymentService.executeCardPayment(authenticatedMember.userId(), request)
		);
	}

	@Operation(summary = "Cancel admin card payment")
	@PostMapping("/payments/card/cancel")
	public ApiResponse<AccountAdminDto.PaymentResponse> cancelCardPayment(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@RequestBody AccountAdminDto.CardPaymentCancelRequest request
	) {
		return ApiResponse.ok(
			"Card payment canceled.",
			adminPaymentService.cancelCardPayment(authenticatedMember.userId(), request)
		);
	}
}

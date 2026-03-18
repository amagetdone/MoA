package com.ssafy.amagetdone.domain.account.api.controller;

import java.util.List;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.application.service.AccountService;
import com.ssafy.amagetdone.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "계좌 관리자 API")
@RestController
@RequestMapping("/v1/admin/accounts")
@SecurityRequirement(name = "bearerAuth")
public class AccountAdminController {

	private final AccountService accountService;

	public AccountAdminController(AccountService accountService) {
		this.accountService = accountService;
	}

	@Operation(summary = "수시입출금 상품 등록")
	@PostMapping("/products")
	public ApiResponse<AccountAdminDto.ProductResponse> registerProduct(@RequestBody AccountAdminDto.ProductCreateRequest request) {
		return ApiResponse.ok("수시입출금 상품을 등록했습니다.", accountService.registerProduct(request));
	}

	@Operation(summary = "수시입출금 상품 목록 조회")
	@GetMapping("/products")
	public ApiResponse<List<AccountAdminDto.ProductResponse>> getProducts() {
		return ApiResponse.ok("수시입출금 상품 목록을 조회했습니다.", accountService.listProducts());
	}

}

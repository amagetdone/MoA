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

	@Operation(summary = "Initialize SSAFY dummy data")
	@PostMapping("/data-init")
	public ApiResponse<AccountAdminDto.DataInitResponse> initialize(@RequestBody AccountAdminDto.DataInitRequest request) {
		return ApiResponse.ok("SSAFY dummy data initialized.", accountService.initializeDummyData(request));
	}
}

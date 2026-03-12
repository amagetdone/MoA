package com.ssafy.amagetdone.global.controller;

import com.ssafy.amagetdone.global.response.ApiResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GlobalHealthCheckController {

	@GetMapping("/api/health")
	public ApiResponse<String> health() {
		return ApiResponse.ok("헬스 체크에 성공했습니다.", "Hello World!");
	}
}

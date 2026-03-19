package com.ssafy.amagetdone.domain.mypage.api.controller;

import com.ssafy.amagetdone.domain.mypage.api.dto.MypageDto;
import com.ssafy.amagetdone.domain.mypage.application.usecase.GetMypageUseCase;
import com.ssafy.amagetdone.global.docs.controller.MypageDocsController;
import com.ssafy.amagetdone.global.response.ApiResponse;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/mypage")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MypageController implements MypageDocsController {

	private final GetMypageUseCase getMypageUseCase;

	@GetMapping
	public ApiResponse<MypageDto.Response> getMypage(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember
	) {
		return ApiResponse.ok(
			"내 정보 요약을 조회했습니다.",
			getMypageUseCase.execute(authenticatedMember.userId())
		);
	}
}

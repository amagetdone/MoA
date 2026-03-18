package com.ssafy.amagetdone.global.docs.controller;

import com.ssafy.amagetdone.domain.mypage.api.dto.MypageDto;
import com.ssafy.amagetdone.global.response.ApiResponse;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "마이페이지 API", description = "내 정보 요약 API")
public interface MypageDocsController {

	@Operation(summary = "내 정보 요약 조회", description = "로그인한 사용자의 프로필 정보와 목표/저축 요약 지표를 함께 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 정보 요약 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.", content = @Content)
	})
	ApiResponse<MypageDto.Response> getMypage(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember
	);
}

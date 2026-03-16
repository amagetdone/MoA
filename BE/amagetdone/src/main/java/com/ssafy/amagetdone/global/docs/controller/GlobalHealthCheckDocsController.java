package com.ssafy.amagetdone.global.docs.controller;

import com.ssafy.amagetdone.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "공통", description = "공통 운영 API")
public interface GlobalHealthCheckDocsController {

	@Operation(summary = "헬스 체크", description = "간단한 상태 확인 응답을 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "헬스 체크 성공")
	})
	ApiResponse<String> health();
}

package com.ssafy.amagetdone.global.docs.controller;

import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyEssentialExpenseDto;
import com.ssafy.amagetdone.global.response.ApiResponse;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "예산", description = "예산 분석 API")
public interface BudgetDocsController {

	@Operation(summary = "주간 필수비 조회", description = "지정한 예산 주차의 필수비 안내를 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주간 필수비 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content)
	})
	ApiResponse<WeeklyEssentialExpenseDto.Response> getWeeklyEssentialExpenses(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "대상 주차, yyyy-Www") String yearWeek
	);
}

package com.ssafy.amagetdone.global.docs.controller;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetAdjustmentDto;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetAllocationTriggerDto;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetDashboardDto;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetHomeDto;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetIncomeDto;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetSummaryDto;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetWeekSetupDto;
import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyEssentialExpenseDetailDto;
import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyEssentialExpenseDto;
import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyFreeSpendingDetailDto;
import com.ssafy.amagetdone.domain.budget.api.dto.WeeklySavingsDetailDto;
import com.ssafy.amagetdone.global.docs.schema.BudgetHomeApiResponseSchema;
import com.ssafy.amagetdone.global.response.ApiResponse;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "예산", description = "예산 분석 API")
public interface BudgetDocsController {

	@Operation(summary = "월급 저장", description = "로그인한 사용자의 현재 기준 월 월급과 기준 월 정보를 저장합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "월급 저장 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content)
	})
	ApiResponse<BudgetIncomeDto.Response> saveIncome(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "월급 저장 요청",
			content = @Content(schema = @Schema(implementation = BudgetIncomeDto.Request.class))
		)
		BudgetIncomeDto.Request request
	);

	@Operation(summary = "월급 조회", description = "로그인한 사용자의 현재 기준 월 월급과 기준 월 정보를 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "월급 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content)
	})
	ApiResponse<BudgetIncomeDto.Response> getIncome(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember
	);

	@Operation(summary = "예산 배분 실행", description = "로그인한 사용자의 현재 기준 월 월급과 예산 데이터를 기반으로 배분을 실행합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "예산 배분 실행 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content)
	})
	ApiResponse<BudgetAllocationTriggerDto.Response> allocateBudget(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember
	);

	@Operation(summary = "홈 대시보드 자유소비 잔액 조회", description = "로그인한 사용자의 현재 예산 주차 자유소비 잔액 정보를 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "홈 대시보드 자유소비 잔액 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content)
	})
	ApiResponse<BudgetDashboardDto.Response> getBudgetDashboard(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember
	);

	@Operation(summary = "홈 화면 통합 조회", description = "로그인한 사용자의 홈 화면 통합 정보를 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "홈 화면 통합 조회 성공",
			content = @Content(schema = @Schema(implementation = BudgetHomeApiResponseSchema.class))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content)
	})
	ApiResponse<BudgetHomeDto.Response> getBudgetHome(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember
	);

	@Operation(summary = "내 예산 요약 지표 조회", description = "로그인한 사용자의 진행/완료 목표 개수와 총 저축 금액을 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 예산 요약 지표 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content)
	})
	ApiResponse<BudgetSummaryDto.Response> getBudgetSummary(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember
	);

	@Operation(summary = "주간 예산 편성 통합 조회", description = "지정한 예산 주차의 예산 편성 요약, 필수비, 저축 배분 정보를 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주간 예산 편성 통합 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content)
	})
	ApiResponse<BudgetWeekSetupDto.Response> getBudgetWeekSetup(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "대상 주차, yyyy-Www") String yearWeek
	);

	@Operation(summary = "주간 예산 수동 조정 저장", description = "지정한 예산 주차에 자유비 또는 저축 목표 수동 조정을 저장하고 반영된 결과를 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주간 예산 수동 조정 저장 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다.", content = @Content),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content)
	})
	ApiResponse<BudgetAdjustmentDto.Response> saveBudgetAdjustment(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "대상 주차, yyyy-Www") String yearWeek,
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "주간 예산 수동 조정 저장 요청",
			content = @Content(schema = @Schema(implementation = BudgetAdjustmentDto.Request.class))
		)
		BudgetAdjustmentDto.Request request
	);

	@Operation(summary = "주간 저축비 상세 조회", description = "지정한 예산 주차의 저축 목표별 계획/확정 배분과 누적 저축 금액을 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주간 저축비 상세 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content)
	})
	ApiResponse<WeeklySavingsDetailDto.Response> getWeeklySavingsDetail(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "대상 주차, yyyy-Www") String yearWeek
	);

	@Operation(summary = "주간 자유비 상세 조회", description = "지정한 예산 주차의 자유비 배정/지출/잔액과 거래 내역을 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주간 자유비 상세 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content)
	})
	ApiResponse<WeeklyFreeSpendingDetailDto.Response> getWeeklyFreeSpendingDetail(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "대상 주차, yyyy-Www") String yearWeek
	);

	@Operation(summary = "주간 필수비 조회", description = "지정한 예산 주차의 필수비 안내를 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주간 필수비 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content)
	})
	ApiResponse<WeeklyEssentialExpenseDto.Response> getWeeklyEssentialExpenses(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "대상 주차, yyyy-Www") String yearWeek
	);

	@Operation(summary = "주간 필수비 상세 조회", description = "지정한 예산 주차의 필수비 카테고리별 계획/지출/잔액과 거래 내역을 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주간 필수비 상세 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content)
	})
	ApiResponse<WeeklyEssentialExpenseDetailDto.Response> getWeeklyEssentialExpenseDetail(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "대상 주차, yyyy-Www") String yearWeek
	);
}

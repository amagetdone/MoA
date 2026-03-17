package com.ssafy.amagetdone.domain.budget.api.controller;

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
import com.ssafy.amagetdone.domain.budget.application.usecase.GetBudgetHomeUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetBudgetIncomeUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetBudgetDashboardUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetBudgetSummaryUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetBudgetWeekSetupUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetWeeklyEssentialExpenseDetailUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetWeeklyEssentialExpensesUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetWeeklyFreeSpendingDetailUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetWeeklySavingsDetailUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.SaveBudgetAdjustmentUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.SaveBudgetIncomeUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.TriggerBudgetAllocationUseCase;
import com.ssafy.amagetdone.global.docs.controller.BudgetDocsController;
import com.ssafy.amagetdone.global.response.ApiResponse;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/budgets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class BudgetController implements BudgetDocsController {

	private final GetBudgetSummaryUseCase getBudgetSummaryUseCase;
	private final GetBudgetDashboardUseCase getBudgetDashboardUseCase;
	private final GetBudgetHomeUseCase getBudgetHomeUseCase;
	private final GetBudgetWeekSetupUseCase getBudgetWeekSetupUseCase;
	private final GetWeeklyEssentialExpenseDetailUseCase getWeeklyEssentialExpenseDetailUseCase;
	private final GetWeeklyEssentialExpensesUseCase getWeeklyEssentialExpensesUseCase;
	private final GetWeeklyFreeSpendingDetailUseCase getWeeklyFreeSpendingDetailUseCase;
	private final GetWeeklySavingsDetailUseCase getWeeklySavingsDetailUseCase;
	private final SaveBudgetAdjustmentUseCase saveBudgetAdjustmentUseCase;
	private final SaveBudgetIncomeUseCase saveBudgetIncomeUseCase;
	private final GetBudgetIncomeUseCase getBudgetIncomeUseCase;
	private final TriggerBudgetAllocationUseCase triggerBudgetAllocationUseCase;

	@PostMapping("/income")
	public ApiResponse<BudgetIncomeDto.Response> saveIncome(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@RequestBody BudgetIncomeDto.Request request
	) {
		return ApiResponse.ok(
			"월급을 저장했습니다.",
			saveBudgetIncomeUseCase.execute(authenticatedMember.userId(), request)
		);
	}

	@GetMapping("/income")
	public ApiResponse<BudgetIncomeDto.Response> getIncome(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember
	) {
		return ApiResponse.ok(
			"월급을 조회했습니다.",
			getBudgetIncomeUseCase.execute(authenticatedMember.userId())
		);
	}

	@PostMapping("/allocate")
	public ApiResponse<BudgetAllocationTriggerDto.Response> allocateBudget(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember
	) {
		triggerBudgetAllocationUseCase.execute(authenticatedMember.userId());
		return ApiResponse.ok(
			"예산 배분을 실행했습니다.",
			new BudgetAllocationTriggerDto.Response(true)
		);
	}

	@GetMapping("/home/dashboard")
	public ApiResponse<BudgetDashboardDto.Response> getBudgetDashboard(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember
	) {
		return ApiResponse.ok(
			"홈 대시보드 자유소비 잔액을 조회했습니다.",
			getBudgetDashboardUseCase.execute(authenticatedMember.userId())
		);
	}

	@GetMapping("/home")
	public ApiResponse<BudgetHomeDto.Response> getBudgetHome(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember
	) {
		return ApiResponse.ok(
			"홈 화면 통합 정보를 조회했습니다.",
			getBudgetHomeUseCase.execute(authenticatedMember.userId())
		);
	}

	@GetMapping("/me/summary")
	public ApiResponse<BudgetSummaryDto.Response> getBudgetSummary(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember
	) {
		return ApiResponse.ok(
			"내 예산 요약 지표를 조회했습니다.",
			getBudgetSummaryUseCase.execute(authenticatedMember.userId())
		);
	}

	@GetMapping("/weeks/{yearWeek}/setup")
	public ApiResponse<BudgetWeekSetupDto.Response> getBudgetWeekSetup(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@PathVariable("yearWeek") String yearWeek
	) {
		return ApiResponse.ok(
			"주간 예산 편성 통합 정보를 조회했습니다.",
			getBudgetWeekSetupUseCase.execute(authenticatedMember.userId(), yearWeek)
		);
	}

	@PostMapping("/weeks/{yearWeek}/adjustments")
	public ApiResponse<BudgetAdjustmentDto.Response> saveBudgetAdjustment(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@PathVariable("yearWeek") String yearWeek,
		@RequestBody BudgetAdjustmentDto.Request request
	) {
		return ApiResponse.ok(
			"주간 예산 수동 조정을 저장했습니다.",
			saveBudgetAdjustmentUseCase.execute(authenticatedMember.userId(), yearWeek, request)
		);
	}

	@GetMapping("/weeks/{yearWeek}/savings")
	public ApiResponse<WeeklySavingsDetailDto.Response> getWeeklySavingsDetail(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@PathVariable("yearWeek") String yearWeek
	) {
		return ApiResponse.ok(
			"주간 저축비 상세 정보를 조회했습니다.",
			getWeeklySavingsDetailUseCase.execute(authenticatedMember.userId(), yearWeek)
		);
	}

	@GetMapping("/weeks/{yearWeek}/free-spending")
	public ApiResponse<WeeklyFreeSpendingDetailDto.Response> getWeeklyFreeSpendingDetail(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@PathVariable("yearWeek") String yearWeek
	) {
		return ApiResponse.ok(
			"주간 자유비 상세 정보를 조회했습니다.",
			getWeeklyFreeSpendingDetailUseCase.execute(authenticatedMember.userId(), yearWeek)
		);
	}

	@GetMapping("/weeks/{yearWeek}/essential-expenses")
	public ApiResponse<WeeklyEssentialExpenseDto.Response> getWeeklyEssentialExpenses(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@PathVariable("yearWeek") String yearWeek
	) {
		return ApiResponse.ok(
			"二쇨컙 ?꾩닔鍮?異붿쿇 ?뺣낫瑜?議고쉶?덉뒿?덈떎.",
			getWeeklyEssentialExpensesUseCase.execute(authenticatedMember.userId(), yearWeek)
		);
	}

	@GetMapping("/weeks/{yearWeek}/essential-expenses/detail")
	public ApiResponse<WeeklyEssentialExpenseDetailDto.Response> getWeeklyEssentialExpenseDetail(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@PathVariable("yearWeek") String yearWeek
	) {
		return ApiResponse.ok(
			"주간 필수비 상세 정보를 조회했습니다.",
			getWeeklyEssentialExpenseDetailUseCase.execute(authenticatedMember.userId(), yearWeek)
		);
	}
}

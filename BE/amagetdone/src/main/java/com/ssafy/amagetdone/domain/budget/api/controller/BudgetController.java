package com.ssafy.amagetdone.domain.budget.api.controller;

import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyEssentialExpenseDto;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetWeeklyEssentialExpensesUseCase;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.global.docs.controller.BudgetDocsController;
import com.ssafy.amagetdone.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/budgets/weeks")
@RequiredArgsConstructor
public class BudgetController implements BudgetDocsController {

	private final JwtAccessTokenReader jwtAccessTokenReader;
	private final GetWeeklyEssentialExpensesUseCase getWeeklyEssentialExpensesUseCase;

	@GetMapping("/{yearWeek}/essential-expenses")
	public ApiResponse<WeeklyEssentialExpenseDto.Response> getWeeklyEssentialExpenses(
		@RequestHeader(name = "Authorization", required = false) String authorization,
		@PathVariable("yearWeek") String yearWeek
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		return ApiResponse.ok(
			"주간 필수비 추천 정보를 조회했습니다.",
			getWeeklyEssentialExpensesUseCase.execute(userId, yearWeek)
		);
	}
}

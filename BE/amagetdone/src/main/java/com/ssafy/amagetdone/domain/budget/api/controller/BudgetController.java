package com.ssafy.amagetdone.domain.budget.api.controller;

import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyEssentialExpenseDto;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetWeeklyEssentialExpensesUseCase;
import com.ssafy.amagetdone.global.docs.controller.BudgetDocsController;
import com.ssafy.amagetdone.global.response.ApiResponse;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/budgets/weeks")
@RequiredArgsConstructor
public class BudgetController implements BudgetDocsController {

	private final GetWeeklyEssentialExpensesUseCase getWeeklyEssentialExpensesUseCase;

	@GetMapping("/{yearWeek}/essential-expenses")
	public ApiResponse<WeeklyEssentialExpenseDto.Response> getWeeklyEssentialExpenses(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@PathVariable("yearWeek") String yearWeek
	) {
		return ApiResponse.ok(
			"二쇨컙 ?꾩닔鍮?異붿쿇 ?뺣낫瑜?議고쉶?덉뒿?덈떎.",
			getWeeklyEssentialExpensesUseCase.execute(authenticatedMember.userId(), yearWeek)
		);
	}
}

package com.ssafy.amagetdone.domain.budget.api.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyEssentialExpenseDto;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetWeeklyEssentialExpensesUseCase;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.global.config.SecurityConfig;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(BudgetController.class)
@Import(SecurityConfig.class)
class BudgetControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtAccessTokenReader jwtAccessTokenReader;

	@MockitoBean
	private GetWeeklyEssentialExpensesUseCase getWeeklyEssentialExpensesUseCase;

	@Test
	void getWeeklyEssentialExpensesReturnsOnlyEssentialExpenseSections() throws Exception {
		given(getWeeklyEssentialExpensesUseCase.execute(1, "2026-W10"))
			.willReturn(new WeeklyEssentialExpenseDto.Response(
				"2026-W10",
				LocalDate.of(2026, 3, 2),
				LocalDate.of(2026, 3, 8),
				3,
				155000L,
				72000L,
				83000L,
				List.of(
					new WeeklyEssentialExpenseDto.CategoryExpense(2, "Subscription", 17000L),
					new WeeklyEssentialExpenseDto.CategoryExpense(12, "Housing", 55000L)
				),
				List.of(new WeeklyEssentialExpenseDto.CategoryExpense(10, "Food", 83000L)),
				LocalDateTime.of(2026, 3, 13, 15, 20)
			));

		mockMvc.perform(apiGet("/budgets/weeks/2026-W10/essential-expenses")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.essentialAmount").value(155000))
			.andExpect(jsonPath("$.data.fixedExpenseCategories[0].categoryName").value("Subscription"))
			.andExpect(jsonPath("$.data.variableExpenseCategories[0].categoryName").value("Food"))
			.andExpect(jsonPath("$.data.savingBudget").doesNotExist())
			.andExpect(jsonPath("$.data.freeSpendable").doesNotExist());
	}

	private MockHttpServletRequestBuilder apiGet(String path) {
		return get("/api" + path).contextPath("/api");
	}
}

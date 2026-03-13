package com.ssafy.amagetdone.domain.transaction.api.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetMonthlyTransactionAnalysisUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetWeeklyTransactionAnalysisUseCase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(TransactionAnalysisReadController.class)
class TransactionAnalysisReadControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtAccessTokenReader jwtAccessTokenReader;

	@MockitoBean
	private GetWeeklyTransactionAnalysisUseCase getWeeklyTransactionAnalysisUseCase;

	@MockitoBean
	private GetMonthlyTransactionAnalysisUseCase getMonthlyTransactionAnalysisUseCase;

	@Test
	void getWeeklyAnalysisReturnsEssentialBreakdownAndCategoryExpenses() throws Exception {
		given(jwtAccessTokenReader.readUserId("Bearer access-token")).willReturn(1);
		given(getWeeklyTransactionAnalysisUseCase.execute(1, "2026-W10"))
			.willReturn(new WeeklyTransactionAnalysisDto.Response(
				"2026-W10",
				LocalDate.of(2026, 3, 2),
				LocalDate.of(2026, 3, 8),
				480000L,
				155000L,
				72000L,
				83000L,
				List.of(
					new WeeklyTransactionAnalysisDto.CategoryExpense(2, "구독료", 17000L),
					new WeeklyTransactionAnalysisDto.CategoryExpense(12, "주거/통신", 55000L),
					new WeeklyTransactionAnalysisDto.CategoryExpense(10, "식비", 83000L)
				),
				LocalDateTime.of(2026, 3, 13, 15, 20)
			));

		mockMvc.perform(apiGet("/transactions/analysis/weeks/2026-W10")
				.header(AUTHORIZATION, "Bearer access-token"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.yearWeek").value("2026-W10"))
			.andExpect(jsonPath("$.data.expenseAmount").value(480000))
			.andExpect(jsonPath("$.data.essentialAmount").value(155000))
			.andExpect(jsonPath("$.data.fixedEssentialAmount").value(72000))
			.andExpect(jsonPath("$.data.variableEssentialAmount").value(83000))
			.andExpect(jsonPath("$.data.categoryExpenses[0].categoryName").value("구독료"));
	}

	@Test
	void getMonthlyAnalysisReturnsMonthlyEssentialAmountDerivedFromWeeklySnapshots() throws Exception {
		given(jwtAccessTokenReader.readUserId("Bearer access-token")).willReturn(1);
		given(getMonthlyTransactionAnalysisUseCase.execute(1, "2026-03"))
			.willReturn(new MonthlyTransactionAnalysisDto.Response(
				"2026-03",
				1900000L,
				680000L,
				430000L,
				250000L,
				List.of(
					new MonthlyTransactionAnalysisDto.CategoryExpense(10, "식비", 750000L, 39.47),
					new MonthlyTransactionAnalysisDto.CategoryExpense(2, "구독료", 68000L, 3.58)
				),
				LocalDateTime.of(2026, 3, 13, 15, 20)
			));

		mockMvc.perform(apiGet("/transactions/analysis/months/2026-03")
				.header(AUTHORIZATION, "Bearer access-token"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.yearMonth").value("2026-03"))
			.andExpect(jsonPath("$.data.expenseAmount").value(1900000))
			.andExpect(jsonPath("$.data.monthlyEssentialAmount").value(680000))
			.andExpect(jsonPath("$.data.fixedEssentialAmount").value(430000))
			.andExpect(jsonPath("$.data.variableEssentialAmount").value(250000))
			.andExpect(jsonPath("$.data.categoryExpenses[0].ratio").value(39.47));
	}

	private MockHttpServletRequestBuilder apiGet(String path) {
		return get("/api" + path).contextPath("/api");
	}
}

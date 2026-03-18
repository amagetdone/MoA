package com.ssafy.amagetdone.domain.transaction.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.vo.UserStatus;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.domain.transaction.api.dto.ExpenseSummaryDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionListDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionSyncService;
import com.ssafy.amagetdone.domain.transaction.application.usecase.ListTransactionsUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.SyncLatestTransactionsUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.TransactionAnalysisResponseFacade;
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

@WebMvcTest(TransactionController.class)
@Import(SecurityConfig.class)
class TransactionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtAccessTokenReader jwtAccessTokenReader;

	@MockitoBean
	private TransactionAnalysisResponseFacade transactionAnalysisResponseFacade;

	@MockitoBean
	private ListTransactionsUseCase listTransactionsUseCase;

	@MockitoBean
	private SyncLatestTransactionsUseCase syncLatestTransactionsUseCase;

	@Test
	void getExpenseSummaryReadsAuthorizationAndReturnsApiResponse() throws Exception {
		given(transactionAnalysisResponseFacade.getExpenseSummary(1, YearMonth.of(2025, 10), YearMonth.of(2026, 3), "MONTH"))
			.willReturn(new ExpenseSummaryDto.Response(
				new ExpenseSummaryDto.Range("2025-10", "2026-03", "MONTH"),
				List.of(new ExpenseSummaryDto.Point("2026-03", 1900000L, 650000L, 1250000L)),
				new ExpenseSummaryDto.Comparison(1900000L, 1720000L, 180000L, 10.47),
				LocalDateTime.of(2026, 3, 13, 15, 20)
			));

		mockMvc.perform(apiGet("/transactions/expenses/summary")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, List.of())
				))
				.param("fromMonth", "2025-10")
				.param("toMonth", "2026-03")
				.param("bucket", "MONTH"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("지출 요약을 조회했습니다."))
			.andExpect(jsonPath("$.data.range.fromMonth").value("2025-10"))
			.andExpect(jsonPath("$.data.points[0].expenseAmount").value(1900000))
			.andExpect(jsonPath("$.data.comparison.changeRate").value(10.47));
	}

	@Test
	void getMonthlyExpenseReadsAuthorizationAndReturnsApiResponse() throws Exception {
		given(transactionAnalysisResponseFacade.getMonthlyExpense(1, YearMonth.of(2026, 3)))
			.willReturn(new MonthlyExpenseDto.Response(
				"2026-03",
				1900000L,
				650000L,
				1250000L,
				2300000L,
				420000L,
				84,
				LocalDateTime.of(2026, 3, 13, 15, 20)
			));

		mockMvc.perform(apiGet("/transactions/expenses/monthly")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, List.of())
				))
				.param("month", "2026-03"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("월간 지출을 조회했습니다."))
			.andExpect(jsonPath("$.data.month").value("2026-03"))
			.andExpect(jsonPath("$.data.totalExpenseAmount").value(1900000))
			.andExpect(jsonPath("$.data.transactionCount").value(84));
	}

	@Test
	void listTransactionsReadsAuthorizationAndReturnsListPayload() throws Exception {
		given(listTransactionsUseCase.execute(eq(1), any(TransactionListDto.Query.class)))
			.willReturn(new TransactionListDto.Response(
				new TransactionListDto.Sync("SUCCEEDED", true, LocalDateTime.of(2026, 3, 13, 15, 18, 11)),
				0,
				20,
				143L,
				List.of(new TransactionListDto.Item(
					10231,
					"CARD",
					"card-10231",
					LocalDateTime.of(2026, 3, 12, 13, 22, 11),
					"Coffee Shop",
					6100L,
					"OUT",
					new TransactionListDto.CategoryView(13, "Cafe"),
					"VARIABLE",
					true,
					null
				))
			));

		mockMvc.perform(apiGet("/transactions")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, List.of())
				))
				.param("fromDate", "2026-03-01")
				.param("toDate", "2026-03-13")
				.param("analysisInclusion", "ALL")
				.param("page", "0")
				.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("거래 내역을 조회했습니다."))
			.andExpect(jsonPath("$.data.sync.triggered").value(true))
			.andExpect(jsonPath("$.data.totalElements").value(143))
			.andExpect(jsonPath("$.data.items[0].merchantText").value("Coffee Shop"));
	}

	@Test
	void analyzeTransactionsReturnsOnboardingResponse() throws Exception {
		given(transactionAnalysisResponseFacade.analyzeTransactions(1, 6))
			.willReturn(sampleOnboardingResult());

		mockMvc.perform(apiPost("/transactions/analysis")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, List.of())
				))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "months": 6,
					  "relabel": false
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("거래 분석을 완료했습니다."))
			.andExpect(jsonPath("$.data.summaryText").value("최근 3개월 소비 분석을 완료했어요."))
			.andExpect(jsonPath("$.data.averageIncome").value(3_000_000))
			.andExpect(jsonPath("$.data.averageExpense").value(1_900_000))
			.andExpect(jsonPath("$.data.categoryExpenses[0].categoryName").value("Food"));
	}

	@Test
	void syncTransactionsReturnsInsertedCounts() throws Exception {
		given(syncLatestTransactionsUseCase.execute(1, 3, true))
			.willReturn(new TransactionSyncService.SyncResult(4, 2));

		mockMvc.perform(apiPost("/transactions/sync")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, List.of())
				))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "months": 3,
					  "forceFullSync": true
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("거래 동기화를 완료했습니다."))
			.andExpect(jsonPath("$.data.insertedAccountCount").value(4))
			.andExpect(jsonPath("$.data.insertedCardCount").value(2));
	}

	@Test
	void getOnboardingAnalysisReturnsStoredResult() throws Exception {
		given(transactionAnalysisResponseFacade.getOnboardingAnalysis(1))
			.willReturn(sampleOnboardingResult());

		mockMvc.perform(apiGet("/transactions/analysis")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("온보딩 분석 결과를 조회했습니다."))
			.andExpect(jsonPath("$.data.summaryText").value("최근 3개월 소비 분석을 완료했어요."))
			.andExpect(jsonPath("$.data.averageIncome").value(3_000_000))
			.andExpect(jsonPath("$.data.averageExpense").value(1_900_000))
			.andExpect(jsonPath("$.data.remainingAmount").value(1_100_000))
			.andExpect(jsonPath("$.data.fixedExpenseRatio").value(65.0))
			.andExpect(jsonPath("$.data.variableExpenseRatio").value(35.0))
			.andExpect(jsonPath("$.data.categoryExpenses[0].categoryName").value("Food"))
			.andExpect(jsonPath("$.data.categoryExpenses[0].amount").value(750_000));
	}

	@Test
	void getWeeklyAnalysisReturnsEssentialBreakdownAndCategoryExpenses() throws Exception {
		given(transactionAnalysisResponseFacade.getWeeklyAnalysis(1, "2026-W10"))
			.willReturn(new WeeklyTransactionAnalysisDto.Response(
				"2026-W10",
				LocalDate.of(2026, 3, 2),
				LocalDate.of(2026, 3, 8),
				480000L,
				155000L,
				72000L,
				83000L,
				List.of(
					new WeeklyTransactionAnalysisDto.CategoryExpense(2, "Subscription", 17000L),
					new WeeklyTransactionAnalysisDto.CategoryExpense(12, "Housing", 55000L),
					new WeeklyTransactionAnalysisDto.CategoryExpense(10, "Food", 83000L)
				),
				LocalDateTime.of(2026, 3, 13, 15, 20)
			));

		mockMvc.perform(apiGet("/transactions/analysis/weeks/2026-W10")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("주간 분석 결과를 조회했습니다."))
			.andExpect(jsonPath("$.data.yearWeek").value("2026-W10"))
			.andExpect(jsonPath("$.data.expenseAmount").value(480000))
			.andExpect(jsonPath("$.data.essentialAmount").value(155000))
			.andExpect(jsonPath("$.data.fixedEssentialAmount").value(72000))
			.andExpect(jsonPath("$.data.variableEssentialAmount").value(83000))
			.andExpect(jsonPath("$.data.categoryExpenses[0].categoryName").value("Subscription"));
	}

	@Test
	void getMonthlyAnalysisReturnsMonthlyEssentialAmountDerivedFromWeeklySnapshots() throws Exception {
		given(transactionAnalysisResponseFacade.getMonthlyAnalysis(1, "2026-03"))
			.willReturn(new MonthlyTransactionAnalysisDto.Response(
				"2026-03",
				1900000L,
				680000L,
				430000L,
				250000L,
				List.of(
					new MonthlyTransactionAnalysisDto.CategoryExpense(10, "Food", 750000L, 39.47),
					new MonthlyTransactionAnalysisDto.CategoryExpense(2, "Subscription", 68000L, 3.58)
				),
				LocalDateTime.of(2026, 3, 13, 15, 20)
			));

		mockMvc.perform(apiGet("/transactions/analysis/months/2026-03")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("월간 분석 결과를 조회했습니다."))
			.andExpect(jsonPath("$.data.yearMonth").value("2026-03"))
			.andExpect(jsonPath("$.data.expenseAmount").value(1900000))
			.andExpect(jsonPath("$.data.monthlyEssentialAmount").value(680000))
			.andExpect(jsonPath("$.data.fixedEssentialAmount").value(430000))
			.andExpect(jsonPath("$.data.variableEssentialAmount").value(250000))
			.andExpect(jsonPath("$.data.categoryExpenses[0].ratio").value(39.47));
	}

	private TransactionAnalysisDto.OnboardingResult sampleOnboardingResult() {
		return new TransactionAnalysisDto.OnboardingResult(
			"최근 3개월 소비 분석을 완료했어요.",
			3_000_000L,
			1_900_000L,
			1_100_000L,
			65.0,
			35.0,
			"지금 흐름이면 저축 여력이 있어요.",
			List.of(
				new TransactionAnalysisDto.CategoryExpense(10, "Food", 750_000L),
				new TransactionAnalysisDto.CategoryExpense(20, "Housing", 550_000L)
			)
		);
	}

	private User user(int id) {
		return User.builder()
			.id(id)
			.status(UserStatus.ACTIVE)
			.email("tester@example.com")
			.nickname("tester")
			.build();
	}

	private MockHttpServletRequestBuilder apiGet(String path) {
		return get("/api" + path).contextPath("/api");
	}

	private MockHttpServletRequestBuilder apiPost(String path) {
		return post("/api" + path).contextPath("/api");
	}
}

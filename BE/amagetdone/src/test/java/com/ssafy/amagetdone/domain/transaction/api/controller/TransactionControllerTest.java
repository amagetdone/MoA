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

import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseGraphDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionListDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyExpenseGraphDto;
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
	void getWeeklyExpenseGraphReadsAuthorizationAndReturnsApiResponse() throws Exception {
		given(transactionAnalysisResponseFacade.getWeeklyExpenseGraph(1, "2026-03-2"))
			.willReturn(new WeeklyExpenseGraphDto.Response(
				"2026-03-2",
				LocalDate.of(2026, 3, 2),
				LocalDate.of(2026, 3, 8),
				190_000L,
				List.of(
					new WeeklyExpenseGraphDto.Point(LocalDate.of(2026, 3, 2), "월", 50_000L),
					new WeeklyExpenseGraphDto.Point(LocalDate.of(2026, 3, 3), "화", 0L)
				),
				LocalDateTime.of(2026, 3, 13, 15, 20)
			));

		mockMvc.perform(apiGet("/transactions/expenses/graphs/weeks/2026-03-2")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("주간 지출 그래프를 조회했습니다."))
			.andExpect(jsonPath("$.data.yearWeek").value("2026-03-2"))
			.andExpect(jsonPath("$.data.weekStartDate").value("2026-03-02"))
			.andExpect(jsonPath("$.data.weekEndDate").value("2026-03-08"))
			.andExpect(jsonPath("$.data.totalExpenseAmount").value(190_000))
			.andExpect(jsonPath("$.data.points[0].date").value("2026-03-02"))
			.andExpect(jsonPath("$.data.points[0].label").value("월"))
			.andExpect(jsonPath("$.data.points[0].expenseAmount").value(50_000));
	}

	@Test
	void getMonthlyExpenseGraphReadsAuthorizationAndReturnsApiResponse() throws Exception {
		given(transactionAnalysisResponseFacade.getMonthlyExpenseGraph(1, "2026-03"))
			.willReturn(new MonthlyExpenseGraphDto.Response(
				"2026-03",
				LocalDate.of(2026, 3, 1),
				LocalDate.of(2026, 3, 31),
				640_000L,
				List.of(
					new MonthlyExpenseGraphDto.Point(
						"2026-03-1",
						LocalDate.of(2026, 3, 2),
						LocalDate.of(2026, 3, 8),
						"3월 1주차",
						120_000L
					),
					new MonthlyExpenseGraphDto.Point(
						"2026-03-2",
						LocalDate.of(2026, 3, 9),
						LocalDate.of(2026, 3, 15),
						"3월 2주차",
						180_000L
					)
				),
				LocalDateTime.of(2026, 3, 13, 15, 20)
			));

		mockMvc.perform(apiGet("/transactions/expenses/graphs/months/2026-03")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("월간 지출 그래프를 조회했습니다."))
			.andExpect(jsonPath("$.data.yearMonth").value("2026-03"))
			.andExpect(jsonPath("$.data.monthStartDate").value("2026-03-01"))
			.andExpect(jsonPath("$.data.monthEndDate").value("2026-03-31"))
			.andExpect(jsonPath("$.data.totalExpenseAmount").value(640_000))
			.andExpect(jsonPath("$.data.points[0].yearWeek").value("2026-03-1"))
			.andExpect(jsonPath("$.data.points[0].weekStartDate").value("2026-03-02"))
			.andExpect(jsonPath("$.data.points[0].weekEndDate").value("2026-03-08"))
			.andExpect(jsonPath("$.data.points[0].label").value("3월 1주차"))
			.andExpect(jsonPath("$.data.points[0].expenseAmount").value(120_000));
	}

	@Test
	void getMonthlyExpenseReadsAuthorizationAndReturnsApiResponse() throws Exception {
		given(transactionAnalysisResponseFacade.getMonthlyExpense(1, YearMonth.of(2026, 3)))
			.willReturn(new MonthlyExpenseDto.Response(
				"2026-03",
				1_900_000L,
				650_000L,
				1_250_000L,
				2_300_000L,
				420_000L,
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
			.andExpect(jsonPath("$.message").value("월간 지출 요약을 조회했습니다."))
			.andExpect(jsonPath("$.data.month").value("2026-03"))
			.andExpect(jsonPath("$.data.totalExpenseAmount").value(1_900_000))
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
					6_100L,
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
					  "months": 6
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
	void analyzeMonthlyTransactionsReturnsMonthlyOnboardingResponse() throws Exception {
		given(transactionAnalysisResponseFacade.analyzeMonthlyTransactions(1, YearMonth.of(2026, 2)))
			.willReturn(new TransactionAnalysisDto.OnboardingResult(
				"2026-02 소비 분석을 완료했어요.",
				2_100_000L,
				1_320_000L,
				780_000L,
				40.0,
				60.0,
				"이번 달은 식비와 교통비가 높았어요.",
				List.of(
					new TransactionAnalysisDto.CategoryExpense(10, "Food", 700_000L),
					new TransactionAnalysisDto.CategoryExpense(1, "Transport", 120_000L)
				)
			));

		mockMvc.perform(apiPost("/transactions/analysis/monthly")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, List.of())
				))
				.param("month", "2026-02"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("월별 거래 분석을 완료했습니다."))
			.andExpect(jsonPath("$.data.summaryText").value("2026-02 소비 분석을 완료했어요."))
			.andExpect(jsonPath("$.data.averageIncome").value(2_100_000))
			.andExpect(jsonPath("$.data.averageExpense").value(1_320_000))
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
		given(transactionAnalysisResponseFacade.getWeeklyAnalysis(1, "2026-03-2"))
			.willReturn(new WeeklyTransactionAnalysisDto.Response(
				"2026-03-2",
				LocalDate.of(2026, 3, 2),
				LocalDate.of(2026, 3, 8),
				152_000L,
				155_000L,
				72_000L,
				83_000L,
				List.of(
					new WeeklyTransactionAnalysisDto.CategoryExpense(2, "Subscription", 17_000L, 12_000L, 5_000L),
					new WeeklyTransactionAnalysisDto.CategoryExpense(12, "Housing", 55_000L, 55_000L, 0L),
					new WeeklyTransactionAnalysisDto.CategoryExpense(10, "Food", 83_000L, 85_000L, -2_000L)
				),
				LocalDateTime.of(2026, 3, 13, 15, 20)
			));

		mockMvc.perform(apiGet("/transactions/analysis/weeks/2026-03-2")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("주간 분석 결과를 조회했습니다."))
			.andExpect(jsonPath("$.data.yearWeek").value("2026-03-2"))
			.andExpect(jsonPath("$.data.expenseAmount").value(152_000))
			.andExpect(jsonPath("$.data.essentialAmount").value(155_000))
			.andExpect(jsonPath("$.data.fixedEssentialAmount").value(72_000))
			.andExpect(jsonPath("$.data.variableEssentialAmount").value(83_000))
			.andExpect(jsonPath("$.data.categoryExpenses[0].categoryName").value("Subscription"))
			.andExpect(jsonPath("$.data.categoryExpenses[0].plannedAmount").value(17_000))
			.andExpect(jsonPath("$.data.categoryExpenses[0].actualSpentAmount").value(12_000))
			.andExpect(jsonPath("$.data.categoryExpenses[0].remainingAmount").value(5_000));
	}

	@Test
	void getWeeklyAnalysisReturnsEmptyStateWhenDataIsMissing() throws Exception {
		given(transactionAnalysisResponseFacade.getWeeklyAnalysis(1, "2026-03-4"))
			.willReturn(new WeeklyTransactionAnalysisDto.Response(
				"2026-03-4",
				null,
				null,
				0L,
				0L,
				0L,
				0L,
				List.of(),
				null
			));

		mockMvc.perform(apiGet("/transactions/analysis/weeks/2026-03-4")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("주간 분석 결과를 조회했습니다."))
			.andExpect(jsonPath("$.data.yearWeek").value("2026-03-4"))
			.andExpect(jsonPath("$.data.expenseAmount").value(0))
			.andExpect(jsonPath("$.data.essentialAmount").value(0))
			.andExpect(jsonPath("$.data.fixedEssentialAmount").value(0))
			.andExpect(jsonPath("$.data.variableEssentialAmount").value(0))
			.andExpect(jsonPath("$.data.categoryExpenses").isArray())
			.andExpect(jsonPath("$.data.categoryExpenses").isEmpty());
	}

	@Test
	void getMonthlyAnalysisReturnsMonthlyEssentialAmountDerivedFromWeeklySnapshots() throws Exception {
		given(transactionAnalysisResponseFacade.getMonthlyAnalysis(1, "2026-03"))
			.willReturn(new MonthlyTransactionAnalysisDto.Response(
				"2026-03",
				1_900_000L,
				680_000L,
				430_000L,
				250_000L,
				List.of(
					new MonthlyTransactionAnalysisDto.CategoryExpense(10, "Food", 750_000L, 39.47),
					new MonthlyTransactionAnalysisDto.CategoryExpense(2, "Subscription", 68_000L, 3.58)
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
			.andExpect(jsonPath("$.data.expenseAmount").value(1_900_000))
			.andExpect(jsonPath("$.data.monthlyEssentialAmount").value(680_000))
			.andExpect(jsonPath("$.data.fixedEssentialAmount").value(430_000))
			.andExpect(jsonPath("$.data.variableEssentialAmount").value(250_000))
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
			"현재 소비 흐름이면 저축 여력이 있어요.",
			List.of(
				new TransactionAnalysisDto.CategoryExpense(10, "Food", 750_000L),
				new TransactionAnalysisDto.CategoryExpense(20, "Housing", 550_000L)
			)
		);
	}

	private MockHttpServletRequestBuilder apiGet(String path) {
		return get("/api" + path).contextPath("/api");
	}

	private MockHttpServletRequestBuilder apiPost(String path) {
		return post("/api" + path).contextPath("/api");
	}
}

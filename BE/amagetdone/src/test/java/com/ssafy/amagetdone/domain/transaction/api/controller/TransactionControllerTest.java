package com.ssafy.amagetdone.domain.transaction.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
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
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisJobDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionListDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetExpenseSummaryUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetMonthlyExpenseUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetMonthlyTransactionAnalysisUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetTransactionAnalysisJobUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetWeeklyTransactionAnalysisUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.ListTransactionsUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.StartTransactionAnalysisJobUseCase;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisJob;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionAnalysisJobStatus;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionAnalysisStep;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtAccessTokenReader jwtAccessTokenReader;

	@MockitoBean
	private GetExpenseSummaryUseCase getExpenseSummaryUseCase;

	@MockitoBean
	private GetMonthlyExpenseUseCase getMonthlyExpenseUseCase;

	@MockitoBean
	private ListTransactionsUseCase listTransactionsUseCase;

	@MockitoBean
	private StartTransactionAnalysisJobUseCase startTransactionAnalysisJobUseCase;

	@MockitoBean
	private GetTransactionAnalysisJobUseCase getTransactionAnalysisJobUseCase;

	@MockitoBean
	private GetWeeklyTransactionAnalysisUseCase getWeeklyTransactionAnalysisUseCase;

	@MockitoBean
	private GetMonthlyTransactionAnalysisUseCase getMonthlyTransactionAnalysisUseCase;

	@Test
	void getExpenseSummaryReadsAuthorizationAndReturnsApiResponse() throws Exception {
		given(jwtAccessTokenReader.readUserId("Bearer access-token")).willReturn(1);
		given(getExpenseSummaryUseCase.execute(1, YearMonth.of(2025, 10), YearMonth.of(2026, 3), "MONTH"))
			.willReturn(new ExpenseSummaryDto.Response(
				new ExpenseSummaryDto.Range("2025-10", "2026-03", "MONTH"),
				List.of(new ExpenseSummaryDto.Point("2026-03", 1900000L, 650000L, 1250000L)),
				new ExpenseSummaryDto.Comparison(1900000L, 1720000L, 180000L, 10.47),
				LocalDateTime.of(2026, 3, 13, 15, 20)
			));

		mockMvc.perform(apiGet("/transactions/expenses/summary")
				.header(AUTHORIZATION, "Bearer access-token")
				.param("fromMonth", "2025-10")
				.param("toMonth", "2026-03")
				.param("bucket", "MONTH"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.range.fromMonth").value("2025-10"))
			.andExpect(jsonPath("$.data.points[0].expenseAmount").value(1900000))
			.andExpect(jsonPath("$.data.comparison.changeRate").value(10.47));
	}

	@Test
	void getMonthlyExpenseReadsAuthorizationAndReturnsApiResponse() throws Exception {
		given(jwtAccessTokenReader.readUserId("Bearer access-token")).willReturn(1);
		given(getMonthlyExpenseUseCase.execute(1, YearMonth.of(2026, 3)))
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
				.header(AUTHORIZATION, "Bearer access-token")
				.param("month", "2026-03"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.month").value("2026-03"))
			.andExpect(jsonPath("$.data.totalExpenseAmount").value(1900000))
			.andExpect(jsonPath("$.data.transactionCount").value(84));
	}

	@Test
	void listTransactionsReadsAuthorizationAndReturnsListPayload() throws Exception {
		given(jwtAccessTokenReader.readUserId("Bearer access-token")).willReturn(1);
		given(listTransactionsUseCase.execute(eq(1), any(TransactionListDto.Query.class)))
			.willReturn(new TransactionListDto.Response(
				new TransactionListDto.Sync("SUCCEEDED", true, LocalDateTime.of(2026, 3, 13, 15, 18, 11)),
				0,
				20,
				143L,
				List.of(new TransactionListDto.Item(
					10231,
					"CARD",
					"5544332211",
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
				.header(AUTHORIZATION, "Bearer access-token")
				.param("fromDate", "2026-03-01")
				.param("toDate", "2026-03-13")
				.param("sourceType", "CARD")
				.param("analysisInclusion", "ALL")
				.param("page", "0")
				.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.sync.triggered").value(true))
			.andExpect(jsonPath("$.data.totalElements").value(143))
			.andExpect(jsonPath("$.data.items[0].merchantText").value("Coffee Shop"));
	}

	@Test
	void createAnalysisJobReturnsAcceptedResponse() throws Exception {
		TransactionAnalysisJob job = TransactionAnalysisJob.builder()
			.id("txn-analysis-20260313-001")
			.user(user(1))
			.requestedMonths(6)
			.status(TransactionAnalysisJobStatus.PENDING)
			.currentStep(TransactionAnalysisStep.SYNCING)
			.requestedAt(LocalDateTime.of(2026, 3, 13, 15, 19))
			.build();
		given(jwtAccessTokenReader.readUserId("Bearer access-token")).willReturn(1);
		given(startTransactionAnalysisJobUseCase.execute(1, 6, false)).willReturn(job);

		mockMvc.perform(apiPost("/transactions/analysis-jobs")
				.header(AUTHORIZATION, "Bearer access-token")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "months": 6,
					  "forceFullSync": false,
					  "relabel": false
					}
					"""))
			.andExpect(status().isAccepted())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.jobId").value("txn-analysis-20260313-001"))
			.andExpect(jsonPath("$.data.status").value("PENDING"))
			.andExpect(jsonPath("$.data.requestedMonths").value(6));
	}

	@Test
	void getAnalysisJobStatusReturnsApiResponse() throws Exception {
		TransactionAnalysisJob job = TransactionAnalysisJob.builder()
			.id("txn-analysis-20260313-001")
			.user(user(1))
			.requestedMonths(6)
			.status(TransactionAnalysisJobStatus.RUNNING)
			.currentStep(TransactionAnalysisStep.CATEGORY_LABELING)
			.requestedAt(LocalDateTime.of(2026, 3, 13, 15, 19))
			.startedAt(LocalDateTime.of(2026, 3, 13, 15, 19, 2))
			.build();
		given(jwtAccessTokenReader.readUserId("Bearer access-token")).willReturn(1);
		given(getTransactionAnalysisJobUseCase.execute("txn-analysis-20260313-001")).willReturn(job);

		mockMvc.perform(apiGet("/transactions/analysis-jobs/txn-analysis-20260313-001")
				.header(AUTHORIZATION, "Bearer access-token"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.jobId").value("txn-analysis-20260313-001"))
			.andExpect(jsonPath("$.data.status").value("RUNNING"))
			.andExpect(jsonPath("$.data.progress.step").value("CATEGORY_LABELING"));
	}

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
					new WeeklyTransactionAnalysisDto.CategoryExpense(2, "Subscription", 17000L),
					new WeeklyTransactionAnalysisDto.CategoryExpense(12, "Housing", 55000L),
					new WeeklyTransactionAnalysisDto.CategoryExpense(10, "Food", 83000L)
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
			.andExpect(jsonPath("$.data.categoryExpenses[0].categoryName").value("Subscription"));
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
					new MonthlyTransactionAnalysisDto.CategoryExpense(10, "Food", 750000L, 39.47),
					new MonthlyTransactionAnalysisDto.CategoryExpense(2, "Subscription", 68000L, 3.58)
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

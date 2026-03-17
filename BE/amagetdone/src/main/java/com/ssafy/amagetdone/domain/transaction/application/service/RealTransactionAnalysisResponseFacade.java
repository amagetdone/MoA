package com.ssafy.amagetdone.domain.transaction.application.service;

import java.time.YearMonth;

import com.ssafy.amagetdone.domain.transaction.api.dto.ExpenseSummaryDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisJobDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetExpenseSummaryUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetMonthlyExpenseUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetMonthlyTransactionAnalysisUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetOnboardingAnalysisResultUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetTransactionAnalysisJobUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetWeeklyTransactionAnalysisUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.StartTransactionAnalysisJobUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.TransactionAnalysisResponseFacade;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisJob;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionAnalysisJobStatus;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
	name = "transaction.analysis-response-mode",
	havingValue = "real",
	matchIfMissing = true
)
public class RealTransactionAnalysisResponseFacade implements TransactionAnalysisResponseFacade {

	private final GetExpenseSummaryUseCase getExpenseSummaryUseCase;
	private final GetMonthlyExpenseUseCase getMonthlyExpenseUseCase;
	private final StartTransactionAnalysisJobUseCase startTransactionAnalysisJobUseCase;
	private final GetTransactionAnalysisJobUseCase getTransactionAnalysisJobUseCase;
	private final GetOnboardingAnalysisResultUseCase getOnboardingAnalysisResultUseCase;
	private final GetWeeklyTransactionAnalysisUseCase getWeeklyTransactionAnalysisUseCase;
	private final GetMonthlyTransactionAnalysisUseCase getMonthlyTransactionAnalysisUseCase;

	@Override
	public TransactionAnalysisJob createAnalysisJob(Integer userId, Integer months, boolean forceFullSync) {
		return startTransactionAnalysisJobUseCase.execute(userId, months, forceFullSync);
	}

	@Override
	public ExpenseSummaryDto.Response getExpenseSummary(Integer userId, YearMonth fromMonth, YearMonth toMonth,
		String bucket) {
		return getExpenseSummaryUseCase.execute(userId, fromMonth, toMonth, bucket);
	}

	@Override
	public MonthlyExpenseDto.Response getMonthlyExpense(Integer userId, YearMonth month) {
		return getMonthlyExpenseUseCase.execute(userId, month);
	}

	@Override
	public TransactionAnalysisJobDto.StatusResponse getAnalysisJobStatus(Integer userId, String jobId) {
		TransactionAnalysisJob job = getTransactionAnalysisJobUseCase.execute(jobId);
		TransactionAnalysisJobDto.OnboardingResult onboardingResult = job.getStatus() == TransactionAnalysisJobStatus.SUCCEEDED
			? getOnboardingAnalysisResultUseCase.execute(userId, job.getFinishedAt())
			: null;
		return TransactionAnalysisJobDto.StatusResponse.from(job, onboardingResult);
	}

	@Override
	public WeeklyTransactionAnalysisDto.Response getWeeklyAnalysis(Integer userId, String yearWeek) {
		return getWeeklyTransactionAnalysisUseCase.execute(userId, yearWeek);
	}

	@Override
	public MonthlyTransactionAnalysisDto.Response getMonthlyAnalysis(Integer userId, String yearMonth) {
		return getMonthlyTransactionAnalysisUseCase.execute(userId, yearMonth);
	}
}

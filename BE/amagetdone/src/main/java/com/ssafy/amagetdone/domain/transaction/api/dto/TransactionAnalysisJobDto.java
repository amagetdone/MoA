package com.ssafy.amagetdone.domain.transaction.api.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisJob;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionAnalysisStep;

public final class TransactionAnalysisJobDto {

	private TransactionAnalysisJobDto() {
	}

	public record CreateRequest(
		Integer months,
		Boolean forceFullSync,
		Boolean relabel
	) {
	}

	public record CreateResponse(
		String jobId,
		String status,
		Integer requestedMonths,
		LocalDateTime requestedAt
	) {
		public static CreateResponse from(TransactionAnalysisJob job) {
			return new CreateResponse(
				job.getId(),
				job.getStatus().name(),
				job.getRequestedMonths(),
				job.getRequestedAt()
			);
		}
	}

	public record StatusResponse(
		String jobId,
		String status,
		Progress progress,
		Integer requestedMonths,
		LocalDateTime requestedAt,
		LocalDateTime startedAt,
		LocalDateTime finishedAt,
		String errorCode,
		String errorMessage,
		OnboardingResult onboardingResult
	) {
		public static StatusResponse from(TransactionAnalysisJob job, OnboardingResult onboardingResult) {
			return new StatusResponse(
				job.getId(),
				job.getStatus().name(),
				Progress.from(job.getCurrentStep()),
				job.getRequestedMonths(),
				job.getRequestedAt(),
				job.getStartedAt(),
				job.getFinishedAt(),
				job.getErrorCode(),
				job.getErrorMessage(),
				onboardingResult
			);
		}
	}

	public record OnboardingResult(
		String summaryText,
		Long averageIncome,
		Long averageExpense,
		Long remainingAmount,
		Double fixedExpenseRatio,
		Double variableExpenseRatio,
		String insightMessage,
		List<CategoryExpense> categoryExpenses
	) {
	}

	public record CategoryExpense(
		Integer categoryId,
		String categoryName,
		Long amount
	) {
	}

	public record Progress(
		String step,
		Integer completedSteps,
		Integer totalSteps
	) {
		private static Progress from(TransactionAnalysisStep step) {
			TransactionAnalysisStep safeStep = step == null ? TransactionAnalysisStep.SYNCING : step;
			return new Progress(
				safeStep.name(),
				safeStep.ordinal() + 1,
				TransactionAnalysisStep.values().length
			);
		}
	}
}

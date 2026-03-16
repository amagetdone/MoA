package com.ssafy.amagetdone.domain.transaction.api.dto;

import java.time.LocalDateTime;

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
		String errorMessage
	) {
		public static StatusResponse from(TransactionAnalysisJob job) {
			return new StatusResponse(
				job.getId(),
				job.getStatus().name(),
				Progress.from(job.getCurrentStep()),
				job.getRequestedMonths(),
				job.getRequestedAt(),
				job.getStartedAt(),
				job.getFinishedAt(),
				job.getErrorCode(),
				job.getErrorMessage()
			);
		}
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

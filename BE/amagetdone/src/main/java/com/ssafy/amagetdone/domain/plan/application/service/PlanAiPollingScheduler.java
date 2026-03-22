package com.ssafy.amagetdone.domain.plan.application.service;

import java.util.List;

import com.ssafy.amagetdone.domain.plan.infra.client.PyanClient;
import com.ssafy.amagetdone.domain.plan.infra.client.dto.PyanGenerateResponse;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanAiPipelineStatus;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;
import com.ssafy.amagetdone.domain.plan.model.vo.PyanTaskStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PlanAiPollingScheduler {

	private static final Logger log = LoggerFactory.getLogger(PlanAiPollingScheduler.class);

	private final PlanRepository planRepository;
	private final PyanClient pyanClient;

	public PlanAiPollingScheduler(PlanRepository planRepository, PyanClient pyanClient) {
		this.planRepository = planRepository;
		this.pyanClient = pyanClient;
	}

	@Scheduled(fixedDelayString = "${plan-ai.status-sync-fixed-delay-millis:30000}")
	@Transactional
	public void syncProcessingPlans() {
		List<Plan> processingPlans = planRepository.findAllByAiPipelineStatusAndDeletedFalseAndStatusNotAndPyanStatusUrlIsNotNull(
			PlanAiPipelineStatus.PROCESSING,
			PlanStatus.CANCELLED
		);

		processingPlans.forEach(this::syncPlan);
	}

	private void syncPlan(Plan plan) {
		try {
			PyanGenerateResponse response = pyanClient.fetchStatus(plan.getPyanStatusUrl());
			PyanTaskStatus pyanStatus = PyanTaskStatus.from(response.status());

			if (isReady(response)) {
				String resolvedVideoUrl = resolvePreferredDownloadUrl(response);
				if (resolvedVideoUrl == null) {
					log.warn("event=PLAN_AI_POLLING_READY_WITHOUT_URL planId={} taskId={}", plan.getId(), plan.getPyanTaskId());
					planRepository.syncPyanProgress(
						plan.getId(),
						plan.getUser().getId(),
						pyanStatus,
						response.originalDownloadUrl(),
						response.compressedDownloadUrl(),
						response.message(),
						PlanStatus.CANCELLED
					);
					return;
				}
				if (planRepository.completeAiSuccess(
					plan.getId(),
					plan.getUser().getId(),
					resolvedVideoUrl,
					pyanStatus,
					response.originalDownloadUrl(),
					response.compressedDownloadUrl(),
					PlanAiPipelineStatus.SUCCEEDED,
					PlanStatus.CANCELLED
				) > 0) {
					log.info("event=PLAN_AI_POLLING_COMPLETED planId={} taskId={} videoUrl={}", plan.getId(), plan.getPyanTaskId(), resolvedVideoUrl);
				}
				return;
			}

			if (isFailure(response)) {
				if (planRepository.markAiFailed(
					plan.getId(),
					plan.getUser().getId(),
					PlanAiPipelineStatus.FAILED,
					pyanStatus,
					response.message(),
					PlanStatus.CANCELLED
				) > 0) {
					log.warn("event=PLAN_AI_POLLING_FAILED planId={} taskId={} status={} message={}",
						plan.getId(), plan.getPyanTaskId(), response.status(), response.message());
				}
				return;
			}

			planRepository.syncPyanProgress(
				plan.getId(),
				plan.getUser().getId(),
				pyanStatus,
				response.originalDownloadUrl(),
				response.compressedDownloadUrl(),
				response.message(),
				PlanStatus.CANCELLED
			);
		} catch (Exception exception) {
			log.warn(
				"event=PLAN_AI_POLLING_SKIPPED planId={} taskId={} message={}",
				plan.getId(),
				plan.getPyanTaskId(),
				exception.getMessage()
			);
		}
	}

	private boolean isReady(PyanGenerateResponse response) {
		return response != null && "ready".equalsIgnoreCase(response.status());
	}

	private boolean isFailure(PyanGenerateResponse response) {
		return response != null && (
			"cancelled".equalsIgnoreCase(response.status()) || "error".equalsIgnoreCase(response.status())
		);
	}

	private String resolvePreferredDownloadUrl(PyanGenerateResponse response) {
		if (response == null) {
			return null;
		}
		if (response.compressedDownloadUrl() != null && !response.compressedDownloadUrl().isBlank()) {
			return response.compressedDownloadUrl();
		}
		if (response.originalDownloadUrl() != null && !response.originalDownloadUrl().isBlank()) {
			return response.originalDownloadUrl();
		}
		return null;
	}
}

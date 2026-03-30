package com.ssafy.amagetdone.domain.plan.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.notification.application.event.PlanAiCompletedEvent;
import com.ssafy.amagetdone.domain.plan.infra.client.PyanClient;
import com.ssafy.amagetdone.domain.plan.infra.client.dto.PyanGenerateResponse;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanAiPipelineStatus;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlanAiPollingSchedulerTest {

	@Mock
	private PlanRepository planRepository;

	@Mock
	private PyanClient pyanClient;

	@Mock
	private ApplicationEventPublisher applicationEventPublisher;

	@Test
	void pollsProcessingPlanAndCompletesItWhenPyanIsReady() {
		Plan plan = processingPlan(21, 3, "https://sketch.pyan.kr/api/status/task-21");
		given(planRepository.findAllByAiPipelineStatusAndDeletedFalseAndStatusNotAndPyanStatusUrlIsNotNull(
			PlanAiPipelineStatus.PROCESSING,
			PlanStatus.CANCELLED
		)).willReturn(List.of(plan));
		given(pyanClient.fetchStatus("https://sketch.pyan.kr/api/status/task-21"))
			.willReturn(response(
				"ready",
				"task-21",
				"plan-21",
				null,
				"https://sketch.pyan.kr/api/status/task-21",
				"https://sketch.pyan.kr/cdn/task-21.mp4",
				"https://sketch.pyan.kr/cdn/task-21.compressed.mp4",
				null,
				null
			));
		given(planRepository.completeAiSuccess(eq(21), eq(3), eq("https://sketch.pyan.kr/cdn/task-21.compressed.mp4"), any(), any(), any()))
			.willReturn(1);

		PlanAiPollingScheduler scheduler = new PlanAiPollingScheduler(planRepository, pyanClient, applicationEventPublisher);

		scheduler.syncProcessingPlans();

		verify(planRepository).completeAiSuccess(
			eq(21),
			eq(3),
			eq("https://sketch.pyan.kr/cdn/task-21.compressed.mp4"),
			any(),
			any(),
			any()
		);
		verify(applicationEventPublisher).publishEvent(
			new PlanAiCompletedEvent(3, 21, null, PlanAiPipelineStatus.SUCCEEDED)
		);
	}

	@Test
	void pollsProcessingPlanAndMarksFailureWhenPyanReportsCancelled() {
		Plan plan = processingPlan(22, 4, "https://sketch.pyan.kr/api/status/task-22");
		given(planRepository.findAllByAiPipelineStatusAndDeletedFalseAndStatusNotAndPyanStatusUrlIsNotNull(
			PlanAiPipelineStatus.PROCESSING,
			PlanStatus.CANCELLED
		)).willReturn(List.of(plan));
		given(pyanClient.fetchStatus("https://sketch.pyan.kr/api/status/task-22"))
			.willReturn(response(
				"cancelled",
				"task-22",
				"plan-22",
				"cancelled upstream",
				"https://sketch.pyan.kr/api/status/task-22",
				null,
				null,
				null,
				null
			));
		given(planRepository.markAiFailed(eq(22), eq(4), any(), any(), eq("cancelled upstream"), any())).willReturn(1);

		PlanAiPollingScheduler scheduler = new PlanAiPollingScheduler(planRepository, pyanClient, applicationEventPublisher);

		scheduler.syncProcessingPlans();

		verify(planRepository).markAiFailed(eq(22), eq(4), any(), any(), eq("cancelled upstream"), any());
		verify(applicationEventPublisher).publishEvent(
			new PlanAiCompletedEvent(4, 22, null, PlanAiPipelineStatus.FAILED)
		);
	}

	@Test
	void pollsProcessingPlanWithoutPublishingWhenStillProcessing() {
		Plan plan = processingPlan(23, 5, "https://sketch.pyan.kr/api/status/task-23");
		given(planRepository.findAllByAiPipelineStatusAndDeletedFalseAndStatusNotAndPyanStatusUrlIsNotNull(
			PlanAiPipelineStatus.PROCESSING,
			PlanStatus.CANCELLED
		)).willReturn(List.of(plan));
		given(pyanClient.fetchStatus("https://sketch.pyan.kr/api/status/task-23"))
			.willReturn(response(
				"processing",
				"task-23",
				"plan-23",
				"still working",
				"https://sketch.pyan.kr/api/status/task-23",
				null,
				null,
				Boolean.TRUE,
				Boolean.FALSE
			));

		PlanAiPollingScheduler scheduler = new PlanAiPollingScheduler(planRepository, pyanClient, applicationEventPublisher);

		scheduler.syncProcessingPlans();

		verify(planRepository).syncPyanProgress(eq(23), eq(5), any(), eq("still working"), any());
		verify(applicationEventPublisher, never()).publishEvent(any());
	}

	private Plan processingPlan(int planId, int userId, String statusUrl) {
		Plan plan = instantiatePlan();
		ReflectionTestUtils.setField(plan, "id", planId);
		ReflectionTestUtils.setField(plan, "user", user(userId));
		ReflectionTestUtils.setField(plan, "status", PlanStatus.WAITING);
		ReflectionTestUtils.setField(plan, "aiPipelineStatus", PlanAiPipelineStatus.PROCESSING);
		ReflectionTestUtils.setField(plan, "pyanStatusUrl", statusUrl);
		return plan;
	}

	private User user(int userId) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", userId);
		return user;
	}

	private Plan instantiatePlan() {
		try {
			var constructor = Plan.class.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private PyanGenerateResponse response(
		String status,
		String taskId,
		String goalId,
		String message,
		String statusUrl,
		String originalDownloadUrl,
		String compressedDownloadUrl,
		Boolean demoFallbackScheduled,
		Boolean demoFallbackApplied
	) {
		return new PyanGenerateResponse(
			status,
			taskId,
			goalId,
			message,
			statusUrl,
			null,
			null,
			null,
			null,
			"https://sketch.pyan.kr/api/goals/" + goalId,
			originalDownloadUrl,
			compressedDownloadUrl,
			null,
			null,
			null,
			null,
			demoFallbackScheduled,
			demoFallbackApplied,
			null,
			null
		);
	}
}

package com.ssafy.amagetdone.domain.plan.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import com.ssafy.amagetdone.domain.member.application.service.MemberProfileImageService;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.infra.client.PlanAiClient;
import com.ssafy.amagetdone.domain.plan.infra.client.PyanClient;
import com.ssafy.amagetdone.domain.plan.infra.client.dto.PyanGenerateResponse;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.global.infra.storage.ImageCategory;
import com.ssafy.amagetdone.global.infra.storage.S3StorageService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlanAiPipelineServiceTest {

	@Mock
	private PlanRepository planRepository;

	@Mock
	private MemberProfileImageService memberProfileImageService;

	@Mock
	private PlanAiClient planAiClient;

	@Mock
	private S3StorageService storageService;

	@Mock
	private PyanClient pyanClient;

	@Mock
	private PlatformTransactionManager transactionManager;

	private PlanAiPipelineService planAiPipelineService;

	private void createServiceWithClock(Clock clock) {
		given(transactionManager.getTransaction(any())).willReturn(new SimpleTransactionStatus());
		planAiPipelineService = new PlanAiPipelineService(
			planRepository,
			memberProfileImageService,
			planAiClient,
			storageService,
			pyanClient,
			clock,
			transactionManager
		);
	}

	@Test
	void startStoresQueuedPyanTaskMetadataAndKeepsPipelineProcessing() {
		createServiceWithClock(Clock.fixed(Instant.parse("2026-03-18T15:30:00Z"), ZoneId.of("Asia/Seoul")));
		Plan plan = instantiatePlan(10, 1, "Europe trip fund", "Summer travel savings");
		ReflectionTestUtils.setField(plan, "startDate", LocalDate.of(2026, 3, 19));
		ReflectionTestUtils.setField(plan, "status", com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus.ACTIVE);
		given(planRepository.findByIdAndUserIdAndDeletedFalse(10, 1)).willReturn(Optional.of(plan));
		stubQueuedStateUpdates(10, 1);
		given(memberProfileImageService.loadProfileImageUrl(1)).willReturn("https://cdn.example.com/profile.png");
		given(planAiClient.generatePlanImage(
			"[Title] Europe trip fund [description] Summer travel savings",
			"https://cdn.example.com/profile.png"
		))
			.willReturn("Z2VuZXJhdGVkLWltYWdl");
		given(storageService.upload(any(), eq(ImageCategory.PLAN))).willReturn("plan/generated.png");
		given(pyanClient.generate(any(byte[].class), eq("plan-10"), any()))
			.willReturn(new PyanGenerateResponse(
				"queued",
				"task-1",
				"plan-10",
				null,
				"https://sketch.pyan.kr/api/status/task-1",
				null,
				null,
				null,
				null,
				"https://sketch.pyan.kr/api/goals/plan-10",
				null,
				null
			));

		planAiPipelineService.start(10, 1);

		assertEquals("PROCESSING", ReflectionTestUtils.getField(plan, "aiPipelineStatus").toString());
		assertEquals("ACTIVE", ReflectionTestUtils.getField(plan, "status").toString());
		assertEquals("plan/generated.png", ReflectionTestUtils.getField(plan, "planUrl"));
		assertEquals("plan-10-pyan-1", ReflectionTestUtils.getField(plan, "pyanRequestKey"));
		assertEquals("QUEUED", ReflectionTestUtils.getField(plan, "pyanStatus").toString());
		assertEquals("https://sketch.pyan.kr/api/status/task-1", ReflectionTestUtils.getField(plan, "pyanStatusUrl"));
		assertNull(ReflectionTestUtils.getField(plan, "videoUrl"));
		verify(memberProfileImageService).loadProfileImageUrl(1);
		verify(planAiClient).generatePlanImage(
			"[Title] Europe trip fund [description] Summer travel savings",
			"https://cdn.example.com/profile.png"
		);
		verify(pyanClient).generate(any(byte[].class), eq("plan-10"), eq("plan-10-pyan-1"));
		verify(planRepository, never()).saveAndFlush(any());
	}

	@Test
	void startCompletesImmediatelyWhenPyanReturnsReadyWithFallbackDownloadUrl() {
		createServiceWithClock(Clock.fixed(Instant.parse("2026-03-18T15:30:00Z"), ZoneId.of("Asia/Seoul")));
		Plan plan = instantiatePlan(11, 1, "Europe trip fund", "Summer travel savings");
		ReflectionTestUtils.setField(plan, "startDate", LocalDate.of(2026, 3, 20));
		given(planRepository.findByIdAndUserIdAndDeletedFalse(11, 1)).willReturn(Optional.of(plan));
		stubSuccessfulStateUpdates(11, 1);
		given(memberProfileImageService.loadProfileImageUrl(1)).willReturn("https://cdn.example.com/profile.png");
		given(planAiClient.generatePlanImage(
			"[Title] Europe trip fund [description] Summer travel savings",
			"https://cdn.example.com/profile.png"
		))
			.willReturn("data:image/png;base64,Z2VuZXJhdGVkLWltYWdl");
		given(storageService.upload(any(), eq(ImageCategory.PLAN))).willReturn("plan/generated.png");
		given(pyanClient.generate(any(byte[].class), eq("plan-11"), any()))
			.willReturn(new PyanGenerateResponse(
				"ready",
				"task-2",
				"plan-11",
				null,
				"https://sketch.pyan.kr/api/status/task-2",
				"https://sketch.pyan.kr/api/video/task-2?variant=original",
				null,
				null,
				"https://sketch.pyan.kr/cdn/original-2.mp4",
				"https://sketch.pyan.kr/api/goals/plan-11",
				"https://sketch.pyan.kr/cdn/original-2.mp4",
				null
			));

		planAiPipelineService.start(11, 1);

		assertEquals("SUCCEEDED", ReflectionTestUtils.getField(plan, "aiPipelineStatus").toString());
		assertEquals("READY", ReflectionTestUtils.getField(plan, "pyanStatus").toString());
		assertEquals("https://sketch.pyan.kr/cdn/original-2.mp4", ReflectionTestUtils.getField(plan, "videoUrl"));
		assertEquals("WAITING", ReflectionTestUtils.getField(plan, "status").toString());
	}

	@Test
	void startMarksFailedWhenPipelineThrowsException() {
		createServiceWithClock(Clock.fixed(Instant.parse("2026-03-18T15:30:00Z"), ZoneId.of("Asia/Seoul")));
		Plan plan = instantiatePlan(12, 1, "Europe trip fund", "Summer travel savings");
		given(planRepository.findByIdAndUserIdAndDeletedFalse(12, 1)).willReturn(Optional.of(plan));
		given(planRepository.markAiProcessing(eq(12), eq(1), any(), any(), any(), any())).willReturn(1);
		given(planRepository.markAiFailed(eq(12), eq(1), any(), any(), any(), any())).willReturn(1);
		given(memberProfileImageService.loadProfileImageUrl(1)).willReturn("https://cdn.example.com/profile.png");
		given(planAiClient.generatePlanImage(
			"[Title] Europe trip fund [description] Summer travel savings",
			"https://cdn.example.com/profile.png"
		)).willThrow(new IllegalStateException("AI unavailable"));

		planAiPipelineService.start(12, 1);

		assertEquals("FAILED", ReflectionTestUtils.getField(plan, "aiPipelineStatus").toString());
		assertEquals(1, ReflectionTestUtils.getField(plan, "aiRetryCount"));
		assertNull(ReflectionTestUtils.getField(plan, "planUrl"));
	}

	@Test
	void startDoesNothingWhenPipelineWasAlreadyStartedForPlan() {
		createServiceWithClock(Clock.fixed(Instant.parse("2026-03-18T15:30:00Z"), ZoneId.of("Asia/Seoul")));
		Plan plan = instantiatePlan(15, 1, "Europe trip fund", "Summer travel savings");
		given(planRepository.findByIdAndUserIdAndDeletedFalse(15, 1)).willReturn(Optional.of(plan));
		given(planRepository.markAiProcessing(eq(15), eq(1), any(), any(), any(), any())).willReturn(0);

		planAiPipelineService.start(15, 1);

		verify(memberProfileImageService, never()).loadProfileImageUrl(any());
		verify(planAiClient, never()).generatePlanImage(any(), any());
		verify(pyanClient, never()).generate(any(), any(), any());
	}

	@Test
	void startMarksFailedWhenInitialPyanRegistrationFails() {
		createServiceWithClock(Clock.fixed(Instant.parse("2026-03-18T15:30:00Z"), ZoneId.of("Asia/Seoul")));
		Plan plan = instantiatePlan(13, 1, "Europe trip fund", "Summer travel savings");
		given(planRepository.findByIdAndUserIdAndDeletedFalse(13, 1)).willReturn(Optional.of(plan));
		given(planRepository.markAiProcessing(eq(13), eq(1), any(), any(), any(), any())).willReturn(1);
		given(planRepository.updateAiImageKey(eq(13), eq(1), eq("plan/generated.png"), any())).willReturn(1);
		given(planRepository.markAiFailed(eq(13), eq(1), any(), any(), any(), any())).willReturn(1);
		given(memberProfileImageService.loadProfileImageUrl(1)).willReturn("https://cdn.example.com/profile.png");
		given(planAiClient.generatePlanImage(
			"[Title] Europe trip fund [description] Summer travel savings",
			"https://cdn.example.com/profile.png"
		))
			.willReturn("Z2VuZXJhdGVkLWltYWdl");
		given(storageService.upload(any(), eq(ImageCategory.PLAN))).willReturn("plan/generated.png");
		given(pyanClient.generate(any(byte[].class), eq("plan-13"), any()))
			.willThrow(new IllegalStateException("Pyan registration failed"));

		planAiPipelineService.start(13, 1);

		assertEquals("FAILED", ReflectionTestUtils.getField(plan, "aiPipelineStatus").toString());
		assertEquals(1, ReflectionTestUtils.getField(plan, "aiRetryCount"));
		assertNull(ReflectionTestUtils.getField(plan, "videoUrl"));
	}

	@Test
	void startDoesNotReactivateCancelledDeletedPlanAfterAsyncCompletion() {
		createServiceWithClock(Clock.fixed(Instant.parse("2026-03-18T15:30:00Z"), ZoneId.of("Asia/Seoul")));
		Plan plan = instantiatePlan(14, 1, "Europe trip fund", "Summer travel savings");
		ReflectionTestUtils.setField(plan, "startDate", LocalDate.of(2026, 3, 18));
		given(planRepository.findByIdAndUserIdAndDeletedFalse(14, 1)).willReturn(Optional.of(plan));
		given(planRepository.markAiProcessing(eq(14), eq(1), any(), any(), any(), any())).willReturn(1);
		given(planRepository.updateAiImageKey(eq(14), eq(1), eq("plan/generated.png"), any())).willReturn(1);
		doAnswer(invocation -> {
			plan.cancelAndDelete();
			return 0;
		}).when(planRepository).registerPyanTask(eq(14), eq(1), any(), any(), any());
		given(memberProfileImageService.loadProfileImageUrl(1)).willReturn("https://cdn.example.com/profile.png");
		given(planAiClient.generatePlanImage(
			"[Title] Europe trip fund [description] Summer travel savings",
			"https://cdn.example.com/profile.png"
		))
			.willReturn("Z2VuZXJhdGVkLWltYWdl");
		given(storageService.upload(any(), eq(ImageCategory.PLAN))).willReturn("plan/generated.png");
		given(pyanClient.generate(any(byte[].class), eq("plan-14"), any()))
			.willReturn(new PyanGenerateResponse(
				"queued",
				"task-14",
				"plan-14",
				null,
				"https://sketch.pyan.kr/api/status/task-14",
				null,
				null,
				null,
				null,
				"https://sketch.pyan.kr/api/goals/plan-14",
				null,
				null
			));
		planAiPipelineService.start(14, 1);

		assertEquals("CANCELLED", ReflectionTestUtils.getField(plan, "status").toString());
		assertEquals("PROCESSING", ReflectionTestUtils.getField(plan, "aiPipelineStatus").toString());
		assertNull(ReflectionTestUtils.getField(plan, "videoUrl"));
	}

	@Test
	void startReusesExistingPyanRequestKeyWhenPresent() {
		createServiceWithClock(Clock.fixed(Instant.parse("2026-03-18T15:30:00Z"), ZoneId.of("Asia/Seoul")));
		Plan plan = instantiatePlan(16, 1, "Europe trip fund", "Summer travel savings");
		ReflectionTestUtils.setField(plan, "pyanRequestKey", "plan-16-pyan-existing");
		given(planRepository.findByIdAndUserIdAndDeletedFalse(16, 1)).willReturn(Optional.of(plan));
		stubQueuedStateUpdates(16, 1);
		given(memberProfileImageService.loadProfileImageUrl(1)).willReturn("https://cdn.example.com/profile.png");
		given(planAiClient.generatePlanImage(
			"[Title] Europe trip fund [description] Summer travel savings",
			"https://cdn.example.com/profile.png"
		)).willReturn("Z2VuZXJhdGVkLWltYWdl");
		given(storageService.upload(any(), eq(ImageCategory.PLAN))).willReturn("plan/generated.png");
		given(pyanClient.generate(any(byte[].class), eq("plan-16"), eq("plan-16-pyan-existing")))
			.willReturn(new PyanGenerateResponse(
				"queued",
				"task-16",
				"plan-16",
				null,
				"https://sketch.pyan.kr/api/status/task-16",
				null,
				null,
				null,
				null,
				"https://sketch.pyan.kr/api/goals/plan-16",
				null,
				null
			));

		planAiPipelineService.start(16, 1);

		assertEquals("plan-16-pyan-existing", ReflectionTestUtils.getField(plan, "pyanRequestKey"));
		verify(pyanClient).generate(any(byte[].class), eq("plan-16"), eq("plan-16-pyan-existing"));
	}

	private Plan instantiatePlan(int planId, int userId, String title, String description) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", userId);
		Plan plan = instantiatePlan();
		ReflectionTestUtils.setField(plan, "id", planId);
		ReflectionTestUtils.setField(plan, "user", user);
		ReflectionTestUtils.setField(plan, "title", title);
		ReflectionTestUtils.setField(plan, "description", description);
		return plan;
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

	private void stubSuccessfulStateUpdates(int planId, int userId) {
		given(planRepository.markAiProcessing(eq(planId), eq(userId), any(), any(), any(), any())).willReturn(1);
		given(planRepository.updateAiImageKey(eq(planId), eq(userId), any(), any())).willReturn(1);
		given(planRepository.registerPyanTask(eq(planId), eq(userId), any(), any(), any())).willReturn(1);
		given(planRepository.completeAiSuccess(eq(planId), eq(userId), any(), any(), any(), any())).willReturn(1);
	}

	private void stubQueuedStateUpdates(int planId, int userId) {
		given(planRepository.markAiProcessing(eq(planId), eq(userId), any(), any(), any(), any())).willReturn(1);
		given(planRepository.updateAiImageKey(eq(planId), eq(userId), any(), any())).willReturn(1);
		given(planRepository.registerPyanTask(eq(planId), eq(userId), any(), any(), any())).willReturn(1);
	}
}

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
	void startMarksProcessingStoresPlanImageAndTracksPyanGenerateResponse() {
		createServiceWithClock(Clock.fixed(Instant.parse("2026-03-18T15:30:00Z"), ZoneId.of("Asia/Seoul")));
		Plan plan = instantiatePlan(10, 1, "Europe trip fund", "Summer travel savings");
		ReflectionTestUtils.setField(plan, "startDate", LocalDate.of(2026, 3, 19));
		ReflectionTestUtils.setField(plan, "status", com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus.ACTIVE);
		given(planRepository.findByIdAndUserIdAndDeletedFalse(10, 1)).willReturn(Optional.of(plan));
		stubSuccessfulStateUpdates(10, 1);
		given(memberProfileImageService.loadProfileImageUrl(1)).willReturn("https://cdn.example.com/profile.png");
		given(planAiClient.generatePlanImage(
			"[Title] Europe trip fund [description] Summer travel savings",
			"https://cdn.example.com/profile.png"
		))
			.willReturn("Z2VuZXJhdGVkLWltYWdl");
		given(storageService.upload(any(), eq(ImageCategory.PLAN))).willReturn("plan/generated.png");
		given(pyanClient.generate("generated-image".getBytes(), "plan-10"))
			.willReturn(new PyanGenerateResponse(
				"ready",
				"task-1",
				"plan-10",
				null,
				"https://sketch.pyan.kr/api/status/task-1",
				"https://sketch.pyan.kr/api/video/task-1?variant=original",
				null,
				null,
				"https://sketch.pyan.kr/cdn/original.mp4",
				"https://sketch.pyan.kr/api/goals/plan-10",
				"https://sketch.pyan.kr/cdn/original.mp4",
				"https://sketch.pyan.kr/cdn/compressed.mp4"
			));

		planAiPipelineService.start(10, 1);

		assertEquals("SUCCEEDED", ReflectionTestUtils.getField(plan, "aiPipelineStatus").toString());
		assertEquals("ACTIVE", ReflectionTestUtils.getField(plan, "status").toString());
		assertEquals("plan/generated.png", ReflectionTestUtils.getField(plan, "planUrl"));
		assertEquals("https://sketch.pyan.kr/cdn/compressed.mp4", ReflectionTestUtils.getField(plan, "videoUrl"));
		verify(memberProfileImageService).loadProfileImageUrl(1);
		verify(planAiClient).generatePlanImage(
			"[Title] Europe trip fund [description] Summer travel savings",
			"https://cdn.example.com/profile.png"
		);
		verify(pyanClient).generate("generated-image".getBytes(), "plan-10");
		verify(planRepository, never()).saveAndFlush(any());
	}

	@Test
	void startDecodesDataUriBase64ImageBeforeUploadAndVideoGeneration() {
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
		given(pyanClient.generate("generated-image".getBytes(), "plan-11"))
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
				"https://sketch.pyan.kr/cdn/compressed-2.mp4"
			));

		planAiPipelineService.start(11, 1);

		verify(pyanClient).generate("generated-image".getBytes(), "plan-11");
		assertEquals("WAITING", ReflectionTestUtils.getField(plan, "status").toString());
	}

	@Test
	void startMarksFailedWhenPipelineThrowsException() {
		createServiceWithClock(Clock.fixed(Instant.parse("2026-03-18T15:30:00Z"), ZoneId.of("Asia/Seoul")));
		Plan plan = instantiatePlan(12, 1, "Europe trip fund", "Summer travel savings");
		given(planRepository.findByIdAndUserIdAndDeletedFalse(12, 1)).willReturn(Optional.of(plan));
		given(planRepository.markAiProcessing(eq(12), eq(1), any(), any())).willReturn(1);
		given(planRepository.markAiFailed(eq(12), eq(1), any(), any())).willReturn(1);
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
	void startMarksFailedWhenPyanResponseDoesNotContainCompressedDownloadUrl() {
		createServiceWithClock(Clock.fixed(Instant.parse("2026-03-18T15:30:00Z"), ZoneId.of("Asia/Seoul")));
		Plan plan = instantiatePlan(13, 1, "Europe trip fund", "Summer travel savings");
		given(planRepository.findByIdAndUserIdAndDeletedFalse(13, 1)).willReturn(Optional.of(plan));
		given(planRepository.markAiProcessing(eq(13), eq(1), any(), any())).willReturn(1);
		given(planRepository.updateAiImageKey(eq(13), eq(1), eq("plan/generated.png"), any())).willReturn(1);
		given(planRepository.markAiFailed(eq(13), eq(1), any(), any())).willReturn(1);
		given(memberProfileImageService.loadProfileImageUrl(1)).willReturn("https://cdn.example.com/profile.png");
		given(planAiClient.generatePlanImage(
			"[Title] Europe trip fund [description] Summer travel savings",
			"https://cdn.example.com/profile.png"
		))
			.willReturn("Z2VuZXJhdGVkLWltYWdl");
		given(storageService.upload(any(), eq(ImageCategory.PLAN))).willReturn("plan/generated.png");
		given(pyanClient.generate("generated-image".getBytes(), "plan-13"))
			.willReturn(new PyanGenerateResponse(
				"ready",
				"task-13",
				"plan-13",
				null,
				"https://sketch.pyan.kr/api/status/task-13",
				"https://sketch.pyan.kr/api/video/task-13?variant=original",
				null,
				null,
				"https://sketch.pyan.kr/cdn/original-13.mp4",
				"https://sketch.pyan.kr/api/goals/plan-13",
				"https://sketch.pyan.kr/cdn/original-13.mp4",
				null
			));

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
		given(planRepository.markAiProcessing(eq(14), eq(1), any(), any())).willReturn(1);
		given(planRepository.updateAiImageKey(eq(14), eq(1), eq("plan/generated.png"), any())).willReturn(1);
		doAnswer(invocation -> {
			plan.cancelAndDelete();
			return 0;
		}).when(planRepository).completeAiSuccess(eq(14), eq(1), eq("https://sketch.pyan.kr/cdn/compressed-14.mp4"), any(), any());
		given(memberProfileImageService.loadProfileImageUrl(1)).willReturn("https://cdn.example.com/profile.png");
		given(planAiClient.generatePlanImage(
			"[Title] Europe trip fund [description] Summer travel savings",
			"https://cdn.example.com/profile.png"
		))
			.willReturn("Z2VuZXJhdGVkLWltYWdl");
		given(storageService.upload(any(), eq(ImageCategory.PLAN))).willReturn("plan/generated.png");
		given(pyanClient.generate("generated-image".getBytes(), "plan-14"))
			.willReturn(new PyanGenerateResponse(
				"ready",
				"task-14",
				"plan-14",
				null,
				"https://sketch.pyan.kr/api/status/task-14",
				"https://sketch.pyan.kr/api/video/task-14?variant=original",
				null,
				null,
				"https://sketch.pyan.kr/cdn/original-14.mp4",
				"https://sketch.pyan.kr/api/goals/plan-14",
				"https://sketch.pyan.kr/cdn/original-14.mp4",
				"https://sketch.pyan.kr/cdn/compressed-14.mp4"
			));
		planAiPipelineService.start(14, 1);

		assertEquals("CANCELLED", ReflectionTestUtils.getField(plan, "status").toString());
		assertEquals("PROCESSING", ReflectionTestUtils.getField(plan, "aiPipelineStatus").toString());
		assertNull(ReflectionTestUtils.getField(plan, "videoUrl"));
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
		given(planRepository.markAiProcessing(eq(planId), eq(userId), any(), any())).willReturn(1);
		given(planRepository.updateAiImageKey(eq(planId), eq(userId), any(), any())).willReturn(1);
		given(planRepository.completeAiSuccess(eq(planId), eq(userId), any(), any(), any())).willReturn(1);
	}
}

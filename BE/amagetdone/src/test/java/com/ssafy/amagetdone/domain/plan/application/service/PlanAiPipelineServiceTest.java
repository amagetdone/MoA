package com.ssafy.amagetdone.domain.plan.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

	@InjectMocks
	private PlanAiPipelineService planAiPipelineService;

	@Test
	void startMarksProcessingStoresPlanImageAndTracksPyanGenerateResponse() {
		Plan plan = instantiatePlan(10, 1, "Europe trip fund", "Summer travel savings");
		given(planRepository.findByIdAndUserId(10, 1)).willReturn(Optional.of(plan));
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
		assertEquals("plan/generated.png", ReflectionTestUtils.getField(plan, "planUrl"));
		assertEquals("https://sketch.pyan.kr/cdn/compressed.mp4", ReflectionTestUtils.getField(plan, "videoUrl"));
		verify(memberProfileImageService).loadProfileImageUrl(1);
		verify(planAiClient).generatePlanImage(
			"[Title] Europe trip fund [description] Summer travel savings",
			"https://cdn.example.com/profile.png"
		);
		verify(pyanClient).generate("generated-image".getBytes(), "plan-10");
	}

	@Test
	void startDecodesDataUriBase64ImageBeforeUploadAndVideoGeneration() {
		Plan plan = instantiatePlan(11, 1, "Europe trip fund", "Summer travel savings");
		given(planRepository.findByIdAndUserId(11, 1)).willReturn(Optional.of(plan));
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
	}

	@Test
	void startMarksFailedWhenPipelineThrowsException() {
		Plan plan = instantiatePlan(12, 1, "Europe trip fund", "Summer travel savings");
		given(planRepository.findByIdAndUserId(12, 1)).willReturn(Optional.of(plan));
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
		Plan plan = instantiatePlan(13, 1, "Europe trip fund", "Summer travel savings");
		given(planRepository.findByIdAndUserId(13, 1)).willReturn(Optional.of(plan));
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
}

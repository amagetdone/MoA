package com.ssafy.amagetdone.domain.plan.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.exception.PlanErrorCode;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.global.infra.storage.ImageCategory;
import com.ssafy.amagetdone.global.infra.storage.S3StorageService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlanImageServiceTest {

	@Mock
	private PlanRepository planRepository;

	@Mock
	private S3StorageService storageService;

	@InjectMocks
	private PlanImageService planImageService;

	@Test
	void replacePlanImageDeletesOldKeyAndStoresNewKey() {
		Plan plan = newPlan(2, 1, "plan/old.png");
		MockMultipartFile file = new MockMultipartFile("image", "new.png", "image/png", "abc".getBytes());
		given(planRepository.findByIdAndUserIdAndDeletedFalse(2, 1)).willReturn(Optional.of(plan));
		given(storageService.upload(file, ImageCategory.PLAN)).willReturn("plan/new.png");

		String objectKey = planImageService.replacePlanImage(1, 2, file);

		verify(storageService).delete("plan/old.png");
		verify(storageService).upload(file, ImageCategory.PLAN);
		assertEquals("plan/new.png", objectKey);
		assertEquals("plan/new.png", plan.getPlanUrl());
	}

	@Test
	void replacePlanImageSkipsDeleteWhenOldKeyMissing() {
		Plan plan = newPlan(2, 1, null);
		MockMultipartFile file = new MockMultipartFile("image", "new.png", "image/png", "abc".getBytes());
		given(planRepository.findByIdAndUserIdAndDeletedFalse(2, 1)).willReturn(Optional.of(plan));
		given(storageService.upload(file, ImageCategory.PLAN)).willReturn("plan/new.png");

		planImageService.replacePlanImage(1, 2, file);

		verify(storageService, never()).delete(any());
	}

	@Test
	void replacePlanImageThrowsWhenPlanMissing() {
		MockMultipartFile file = new MockMultipartFile("image", "new.png", "image/png", "abc".getBytes());
		given(planRepository.findByIdAndUserIdAndDeletedFalse(2, 1)).willReturn(Optional.empty());

		PlanException exception = assertThrows(
			PlanException.class,
			() -> planImageService.replacePlanImage(1, 2, file)
		);

		assertEquals(PlanErrorCode.PLAN_NOT_FOUND, exception.getErrorCode());
	}

	@Test
	void loadPlanImageBytesReadsStoredKey() {
		Plan plan = newPlan(2, 1, "plan/current.png");
		given(planRepository.findByIdAndUserIdAndDeletedFalse(2, 1)).willReturn(Optional.of(plan));
		given(storageService.loadBytes("plan/current.png")).willReturn("abc".getBytes());

		byte[] bytes = planImageService.loadPlanImageBytes(1, 2);

		assertEquals("abc", new String(bytes));
		verify(storageService).loadBytes("plan/current.png");
	}

	private Plan newPlan(int planId, int userId, String planUrl) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", userId);

		Plan plan = instantiatePlan();
		ReflectionTestUtils.setField(plan, "id", planId);
		ReflectionTestUtils.setField(plan, "user", user);
		ReflectionTestUtils.setField(plan, "planUrl", planUrl);
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

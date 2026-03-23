package com.ssafy.amagetdone.domain.plan.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanUpdateRequest;
import com.ssafy.amagetdone.domain.plan.application.event.PlanCreatedEvent;
import com.ssafy.amagetdone.domain.plan.exception.PlanErrorCode;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanAiPipelineStatus;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;
import com.ssafy.amagetdone.domain.plan.model.vo.PyanTaskStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UpdatePlanUseCaseTest {

	@Mock
	private PlanRepository planRepository;

	@Mock
	private ApplicationEventPublisher applicationEventPublisher;

	@InjectMocks
	private UpdatePlanUseCase updatePlanUseCase;

	@Test
	void updatesMetadataResetsAiFieldsAndRepublishesPlanCreatedEvent() {
		Plan plan = plan(10, 1, PlanAiPipelineStatus.SUCCEEDED);
		ReflectionTestUtils.setField(plan, "planUrl", "plan/image.png");
		ReflectionTestUtils.setField(plan, "videoUrl", "plan/video.mp4");
		ReflectionTestUtils.setField(plan, "pyanStatus", PyanTaskStatus.READY);
		ReflectionTestUtils.setField(plan, "pyanStatusUrl", "https://pyan/status/10");
		ReflectionTestUtils.setField(plan, "pyanLastError", "old error");
		ReflectionTestUtils.setField(plan, "pyanRequestKey", "plan-10-pyan-1");

		given(planRepository.findByIdAndUserIdAndDeletedFalse(10, 1)).willReturn(Optional.of(plan));

		updatePlanUseCase.execute(1, 10, new PlanUpdateRequest("Updated title", "Updated description"));

		assertEquals("Updated title", plan.getTitle());
		assertEquals("Updated description", plan.getDescription());
		assertEquals(PlanAiPipelineStatus.PENDING, plan.getAiPipelineStatus());
		assertNull(plan.getPlanUrl());
		assertNull(plan.getVideoUrl());
		assertNull(plan.getPyanStatus());
		assertNull(plan.getPyanStatusUrl());
		assertNull(plan.getPyanLastError());
		assertNull(plan.getPyanRequestKey());
		assertEquals(1, plan.getAiRetryCount());
		verify(applicationEventPublisher).publishEvent(new PlanCreatedEvent(10, 1));
	}

	@Test
	void rejectsUpdateWhenAiPipelineIsProcessing() {
		Plan plan = plan(10, 1, PlanAiPipelineStatus.PROCESSING);

		given(planRepository.findByIdAndUserIdAndDeletedFalse(10, 1)).willReturn(Optional.of(plan));

		PlanException exception = assertThrows(PlanException.class,
			() -> updatePlanUseCase.execute(1, 10, new PlanUpdateRequest("Updated title", "Updated description")));

		assertEquals(PlanErrorCode.PLAN_UPDATE_NOT_ALLOWED, exception.getErrorCode());
	}

	private Plan plan(int id, int userId, PlanAiPipelineStatus aiPipelineStatus) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", userId);

		Plan plan = instantiatePlan();
		ReflectionTestUtils.setField(plan, "id", id);
		ReflectionTestUtils.setField(plan, "user", user);
		ReflectionTestUtils.setField(plan, "title", "Original title");
		ReflectionTestUtils.setField(plan, "description", "Original description");
		ReflectionTestUtils.setField(plan, "status", PlanStatus.ACTIVE);
		ReflectionTestUtils.setField(plan, "aiPipelineStatus", aiPipelineStatus);
		ReflectionTestUtils.setField(plan, "aiRetryCount", 0);
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

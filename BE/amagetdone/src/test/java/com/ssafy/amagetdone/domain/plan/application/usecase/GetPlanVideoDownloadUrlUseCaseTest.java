package com.ssafy.amagetdone.domain.plan.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import com.ssafy.amagetdone.domain.plan.exception.PlanErrorCode;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanAiPipelineStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GetPlanVideoDownloadUrlUseCaseTest {

	@Mock
	private PlanRepository planRepository;

	@InjectMocks
	private GetPlanVideoDownloadUrlUseCase getPlanVideoDownloadUrlUseCase;

	@Test
	void returnsVideoUrlWhenPlanHasVideo() {
		Plan plan = instantiatePlan();
		ReflectionTestUtils.setField(plan, "videoUrl", "https://cdn.example.com/plan-10.mp4");

		given(planRepository.findByIdAndUserIdAndDeletedFalse(10, 1)).willReturn(Optional.of(plan));

		String result = getPlanVideoDownloadUrlUseCase.execute(1, 10);

		assertEquals("https://cdn.example.com/plan-10.mp4", result);
	}

	@Test
	void throwsNotReadyWhenPlanVideoIsStillProcessing() {
		Plan plan = instantiatePlan();
		ReflectionTestUtils.setField(plan, "aiPipelineStatus", PlanAiPipelineStatus.PROCESSING);
		given(planRepository.findByIdAndUserIdAndDeletedFalse(10, 1)).willReturn(Optional.of(plan));

		PlanException exception = assertThrows(
			PlanException.class,
			() -> getPlanVideoDownloadUrlUseCase.execute(1, 10)
		);

		assertEquals(PlanErrorCode.PLAN_VIDEO_NOT_READY, exception.getErrorCode());
	}

	@Test
	void throwsGenerationFailedWhenPlanVideoGenerationFailed() {
		Plan plan = instantiatePlan();
		ReflectionTestUtils.setField(plan, "aiPipelineStatus", PlanAiPipelineStatus.FAILED);
		given(planRepository.findByIdAndUserIdAndDeletedFalse(10, 1)).willReturn(Optional.of(plan));

		PlanException exception = assertThrows(
			PlanException.class,
			() -> getPlanVideoDownloadUrlUseCase.execute(1, 10)
		);

		assertEquals(PlanErrorCode.PLAN_VIDEO_GENERATION_FAILED, exception.getErrorCode());
	}

	@Test
	void throwsNotFoundWhenSucceededPlanStillHasNoVideoUrl() {
		Plan plan = instantiatePlan();
		ReflectionTestUtils.setField(plan, "aiPipelineStatus", PlanAiPipelineStatus.SUCCEEDED);
		given(planRepository.findByIdAndUserIdAndDeletedFalse(10, 1)).willReturn(Optional.of(plan));

		PlanException exception = assertThrows(
			PlanException.class,
			() -> getPlanVideoDownloadUrlUseCase.execute(1, 10)
		);

		assertEquals(PlanErrorCode.PLAN_VIDEO_NOT_FOUND, exception.getErrorCode());
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

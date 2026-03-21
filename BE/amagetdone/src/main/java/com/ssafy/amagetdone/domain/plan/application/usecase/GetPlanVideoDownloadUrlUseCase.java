package com.ssafy.amagetdone.domain.plan.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.amagetdone.domain.plan.exception.PlanErrorCode;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanAiPipelineStatus;

@Service
public class GetPlanVideoDownloadUrlUseCase {

	private final PlanRepository planRepository;

	public GetPlanVideoDownloadUrlUseCase(PlanRepository planRepository) {
		this.planRepository = planRepository;
	}

	@Transactional(readOnly = true)
	public String execute(Integer userId, Integer planId) {
		Plan plan = planRepository.findByIdAndUserIdAndDeletedFalse(planId, userId)
			.orElseThrow(() -> new PlanException(PlanErrorCode.PLAN_NOT_FOUND));

		if (plan.getVideoUrl() == null || plan.getVideoUrl().isBlank()) {
			if (plan.getAiPipelineStatus() == PlanAiPipelineStatus.PENDING
				|| plan.getAiPipelineStatus() == PlanAiPipelineStatus.PROCESSING) {
				throw new PlanException(PlanErrorCode.PLAN_VIDEO_NOT_READY);
			}
			if (plan.getAiPipelineStatus() == PlanAiPipelineStatus.FAILED) {
				throw new PlanException(PlanErrorCode.PLAN_VIDEO_GENERATION_FAILED);
			}
			throw new PlanException(PlanErrorCode.PLAN_VIDEO_NOT_FOUND);
		}

		return plan.getVideoUrl();
	}
}

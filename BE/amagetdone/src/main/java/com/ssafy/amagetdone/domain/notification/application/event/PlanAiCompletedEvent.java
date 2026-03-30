package com.ssafy.amagetdone.domain.notification.application.event;

import com.ssafy.amagetdone.domain.plan.model.vo.PlanAiPipelineStatus;

public record PlanAiCompletedEvent(
	Integer userId,
	Integer planId,
	String planTitle,
	PlanAiPipelineStatus aiPipelineStatus
) {
}

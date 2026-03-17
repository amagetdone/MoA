package com.ssafy.amagetdone.domain.plan.application.event;

public record PlanCreatedEvent(
	Integer planId,
	Integer userId
) {
}

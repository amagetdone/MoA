package com.ssafy.amagetdone.domain.plan.application.listener;

import com.ssafy.amagetdone.domain.plan.application.event.PlanCreatedEvent;
import com.ssafy.amagetdone.domain.plan.application.service.PlanAiPipelineService;
import com.ssafy.amagetdone.domain.plan.infra.client.PlanAiClient;
import com.ssafy.amagetdone.domain.plan.infra.client.PyanClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnBean({ PlanAiClient.class, PyanClient.class })
public class PlanCreatedEventListener {

	private final PlanAiPipelineService planAiPipelineService;

	public PlanCreatedEventListener(PlanAiPipelineService planAiPipelineService) {
		this.planAiPipelineService = planAiPipelineService;
	}

	@Async("planAiTaskExecutor")
	@TransactionalEventListener
	public void handle(PlanCreatedEvent event) {
		planAiPipelineService.start(event.planId(), event.userId());
	}
}

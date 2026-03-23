package com.ssafy.amagetdone.domain.plan.application.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class PlanAiAsyncConfigTest {

	@Test
	void planAiTaskExecutorUsesConfiguredConcurrencyLimits() {
		PlanAiAsyncProperties properties = new PlanAiAsyncProperties();
		properties.setCorePoolSize(2);
		properties.setMaxPoolSize(2);
		properties.setQueueCapacity(8);
		properties.setThreadNamePrefix("plan-ai-test-");

		PlanAiAsyncConfig config = new PlanAiAsyncConfig(properties);

		ThreadPoolTaskExecutor executor = config.planAiTaskExecutor();

		assertEquals(2, executor.getCorePoolSize());
		assertEquals(2, executor.getMaxPoolSize());
		assertEquals(8, executor.getThreadPoolExecutor().getQueue().remainingCapacity() + executor.getThreadPoolExecutor().getQueue().size());
		assertEquals("plan-ai-test-", executor.getThreadNamePrefix());
	}
}

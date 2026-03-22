package com.ssafy.amagetdone.domain.plan.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class PlanAiAsyncConfig {

	private final PlanAiAsyncProperties properties;

	public PlanAiAsyncConfig(PlanAiAsyncProperties properties) {
		this.properties = properties;
	}

	@Bean(name = "planAiTaskExecutor")
	public ThreadPoolTaskExecutor planAiTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(properties.getCorePoolSize());
		executor.setMaxPoolSize(properties.getMaxPoolSize());
		executor.setQueueCapacity(properties.getQueueCapacity());
		executor.setThreadNamePrefix(properties.getThreadNamePrefix());
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.initialize();
		return executor;
	}
}

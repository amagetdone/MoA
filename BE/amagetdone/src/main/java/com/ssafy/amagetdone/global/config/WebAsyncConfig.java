package com.ssafy.amagetdone.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebAsyncConfig implements WebMvcConfigurer {

	private final int corePoolSize;
	private final int maxPoolSize;
	private final int queueCapacity;
	private final long timeoutMillis;

	public WebAsyncConfig(
		@Value("${app.mvc-async.core-pool-size:4}") int corePoolSize,
		@Value("${app.mvc-async.max-pool-size:16}") int maxPoolSize,
		@Value("${app.mvc-async.queue-capacity:200}") int queueCapacity,
		@Value("${app.mvc-async.timeout-millis:0}") long timeoutMillis
	) {
		this.corePoolSize = corePoolSize;
		this.maxPoolSize = maxPoolSize;
		this.queueCapacity = queueCapacity;
		this.timeoutMillis = timeoutMillis;
	}

	@Bean(name = "mvcAsyncTaskExecutor")
	public ThreadPoolTaskExecutor mvcAsyncTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setThreadNamePrefix("mvc-async-");
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.initialize();
		return executor;
	}

	@Bean(name = "mvcAsyncSecurityTaskExecutor")
	public AsyncTaskExecutor mvcAsyncSecurityTaskExecutor(ThreadPoolTaskExecutor mvcAsyncTaskExecutor) {
		return new DelegatingSecurityContextAsyncTaskExecutor(mvcAsyncTaskExecutor);
	}

	@Override
	public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
		configurer.setTaskExecutor(mvcAsyncSecurityTaskExecutor(mvcAsyncTaskExecutor()));
		configurer.setDefaultTimeout(timeoutMillis);
	}
}

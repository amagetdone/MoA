package com.ssafy.amagetdone.domain.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PushNotificationServiceConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void registersNoopServiceWhenFirebaseMessagingBeanIsMissing() {
		contextRunner.withUserConfiguration(NoopPushNotificationService.class)
			.run(context -> {
				assertThat(context).hasSingleBean(PushNotificationService.class);
				assertThat(context).hasSingleBean(NoopPushNotificationService.class);
			});
	}
}

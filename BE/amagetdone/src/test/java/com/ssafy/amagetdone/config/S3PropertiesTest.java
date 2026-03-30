package com.ssafy.amagetdone.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ssafy.amagetdone.global.config.S3Properties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class S3PropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(TestConfiguration.class)
		.withPropertyValues(
			"cloud.aws.credentials.access-key=test-access-key",
			"cloud.aws.credentials.secret-key=test-secret-key",
			"cloud.aws.region.static-region=ap-northeast-2",
			"cloud.aws.s3.bucket=test-bucket"
		);

	@Test
	void bindsS3Properties() {
		contextRunner.run(context -> {
			S3Properties properties = context.getBean(S3Properties.class);

			assertEquals("test-access-key", properties.getCredentials().getAccessKey());
			assertEquals("test-secret-key", properties.getCredentials().getSecretKey());
			assertEquals("ap-northeast-2", properties.getRegion().getStaticRegion());
			assertEquals("test-bucket", properties.getS3().getBucket());
		});
	}

	@Configuration
	@EnableConfigurationProperties(S3Properties.class)
	static class TestConfiguration {
	}
}

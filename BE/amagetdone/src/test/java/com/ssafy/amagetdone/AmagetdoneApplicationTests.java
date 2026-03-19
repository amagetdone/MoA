package com.ssafy.amagetdone;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.util.Map;

import javax.sql.DataSource;

import com.ssafy.amagetdone.global.config.EnvLoader;
import com.ssafy.amagetdone.support.TestSystemPropertySupport;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AmagetdoneApplicationTests {

	private static Map<String, String> springProfileProperties;

	@BeforeAll
	static void loadEnv() {
		springProfileProperties = TestSystemPropertySupport.captureSpringProfileProperties();
		EnvLoader.loadRequiredEnv(Path.of("").toAbsolutePath());
	}

	@AfterAll
	static void restoreEnv() {
		TestSystemPropertySupport.restore(springProfileProperties);
	}

	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoadsWithDataSource() {
		assertNotNull(dataSource);
	}

}

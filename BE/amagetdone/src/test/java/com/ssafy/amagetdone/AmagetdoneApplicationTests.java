package com.ssafy.amagetdone;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;

import javax.sql.DataSource;

import com.ssafy.amagetdone.config.EnvLoader;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AmagetdoneApplicationTests {

	@BeforeAll
	static void loadEnv() {
		EnvLoader.loadRequiredEnv(Path.of("").toAbsolutePath());
	}

	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoadsWithDataSource() {
		assertNotNull(dataSource);
	}

}

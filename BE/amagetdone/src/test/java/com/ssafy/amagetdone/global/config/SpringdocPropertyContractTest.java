package com.ssafy.amagetdone.global.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class SpringdocPropertyContractTest {

	@Test
	void applicationPropertiesEnableFullyQualifiedSchemaNames() throws Exception {
		Properties properties = new Properties();

		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.properties")) {
			assertNotNull(inputStream);
			properties.load(inputStream);
		}

		assertEquals("true", properties.getProperty("springdoc.use-fqn"));
	}
}

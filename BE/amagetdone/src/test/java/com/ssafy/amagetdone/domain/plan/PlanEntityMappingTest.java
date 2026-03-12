package com.ssafy.amagetdone.domain.plan;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import com.ssafy.amagetdone.config.EnvLoader;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@SpringBootTest
@ActiveProfiles("test")
class PlanEntityMappingTest {

	private static final String BASE_PACKAGE = "com.ssafy.amagetdone.domain.plan.model.entity.";

	@Autowired
	private ApplicationContext applicationContext;

	@BeforeAll
	static void loadEnv() {
		EnvLoader.loadRequiredEnv(Path.of("").toAbsolutePath());
	}

	@Test
	void planEntitiesAreDiscoverable() throws ClassNotFoundException {
		assertEntityClass("Plan", "plan");
		assertEntityClass("PlanWeekAllocation", "plan_week_allocation");
	}

	@Test
	void planRepositoriesAreRegistered() {
		assertTrue(applicationContext.containsBeanDefinition("planRepository"));
		assertTrue(applicationContext.containsBeanDefinition("planWeekAllocationRepository"));
	}

	private void assertEntityClass(String simpleName, String tableName) throws ClassNotFoundException {
		Class<?> entityClass = Class.forName(BASE_PACKAGE + simpleName);
		assertNotNull(entityClass.getAnnotation(Entity.class));
		Table table = entityClass.getAnnotation(Table.class);
		assertNotNull(table);
		assertTrue(tableName.equals(table.name()));
	}
}

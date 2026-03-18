package com.ssafy.amagetdone.domain.plan;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;

import com.ssafy.amagetdone.global.config.EnvLoader;
import com.ssafy.amagetdone.support.TestSystemPropertySupport;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanAiPipelineStatus;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;

@SpringBootTest
@ActiveProfiles("test")
class PlanEntityMappingTest {

	private static final String BASE_PACKAGE = "com.ssafy.amagetdone.domain.plan.model.entity.";
	private static Map<String, String> springProfileProperties;

	@Autowired
	private ApplicationContext applicationContext;

	@BeforeAll
	static void loadEnv() {
		springProfileProperties = TestSystemPropertySupport.captureSpringProfileProperties();
		EnvLoader.loadRequiredEnv(Path.of("").toAbsolutePath());
	}

	@AfterAll
	static void restoreEnv() {
		TestSystemPropertySupport.restore(springProfileProperties);
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

	@Test
	void planWeekAllocationIdUsesSequenceGeneration() throws Exception {
		Field idField = PlanWeekAllocation.class.getDeclaredField("id");
		GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
		assertNotNull(generatedValue);
		assertSame(GenerationType.SEQUENCE, generatedValue.strategy());
	}

	@Test
	void planBuilderUsesAiPipelineDefaults() {
		Plan plan = Plan.builder().build();

		assertSame(PlanAiPipelineStatus.PENDING, plan.getAiPipelineStatus());
		assertSame(0, plan.getAiRetryCount());
	}

	private void assertEntityClass(String simpleName, String tableName) throws ClassNotFoundException {
		Class<?> entityClass = Class.forName(BASE_PACKAGE + simpleName);
		assertNotNull(entityClass.getAnnotation(Entity.class));
		Table table = entityClass.getAnnotation(Table.class);
		assertNotNull(table);
		assertTrue(tableName.equals(table.name()));
	}
}

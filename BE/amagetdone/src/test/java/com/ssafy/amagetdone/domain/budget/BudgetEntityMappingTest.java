package com.ssafy.amagetdone.domain.budget;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import com.ssafy.amagetdone.global.config.EnvLoader;
import com.ssafy.amagetdone.support.TestSystemPropertySupport;

import org.junit.jupiter.api.AfterAll;
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
class BudgetEntityMappingTest {

	private static final String ENTITY_PACKAGE = "com.ssafy.amagetdone.domain.budget.model.entity.";
	private static final String ENUM_PACKAGE = "com.ssafy.amagetdone.domain.budget.model.vo.";
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
	void budgetEntitiesAreDiscoverable() throws ClassNotFoundException {
		assertEntityClass("BudgetMonth", "budget_month");
		assertEntityClass("EssentialExpenseMonth", "essential_expense_month");
		assertEntityClass("BudgetWeek", "budget_week");
		assertEntityClass("EssentialWeekAllocation", "essential_week_allocation");
		assertEntityClass("BudgetAdjustment", "budget_adjustment");
		assertEntityClass("BudgetRollover", "budget_rollover");
	}

	@Test
	void budgetRepositoriesAreRegistered() {
		assertTrue(applicationContext.containsBeanDefinition("budgetMonthRepository"));
		assertTrue(applicationContext.containsBeanDefinition("essentialExpenseMonthRepository"));
		assertTrue(applicationContext.containsBeanDefinition("budgetWeekRepository"));
		assertTrue(applicationContext.containsBeanDefinition("essentialWeekAllocationRepository"));
		assertTrue(applicationContext.containsBeanDefinition("budgetAdjustmentRepository"));
		assertTrue(applicationContext.containsBeanDefinition("budgetRolloverRepository"));
	}

	@Test
	void budgetAdjustmentTypeContainsApprovedValues() throws ClassNotFoundException {
		Class<?> enumClass = Class.forName(ENUM_PACKAGE + "BudgetAdjustmentType");
		Object[] constants = enumClass.getEnumConstants();
		assertNotNull(constants);
		assertArrayEquals(
			new String[] {
				"FREE_BUDGET_ADD",
				"FREE_BUDGET_SUBTRACT",
				"PLAN_ADD",
				"PLAN_SUBTRACT",
				"PLAN_DELAY",
				"PLAN_EARLY_CLOSE",
				"ROLLOVER_APPLIED"
			},
			Arrays.stream(constants).map(Object::toString).toArray(String[]::new)
		);
	}

	private void assertEntityClass(String simpleName, String tableName) throws ClassNotFoundException {
		Class<?> entityClass = Class.forName(ENTITY_PACKAGE + simpleName);
		assertNotNull(entityClass.getAnnotation(Entity.class));
		Table table = entityClass.getAnnotation(Table.class);
		assertNotNull(table);
		assertTrue(tableName.equals(table.name()));
	}
}

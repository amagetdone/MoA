package com.ssafy.amagetdone.domain.budget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Path;

import com.ssafy.amagetdone.config.EnvLoader;
import com.ssafy.amagetdone.domain.asset.model.entity.Category;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@SpringBootTest
@ActiveProfiles("test")
class EssentialExpenseSnapshotEntityMappingTest {

	private static final String ENTITY_PACKAGE = "com.ssafy.amagetdone.domain.budget.model.entity.";

	@Autowired
	private ApplicationContext applicationContext;

	@BeforeAll
	static void loadEnv() {
		EnvLoader.loadRequiredEnv(Path.of("").toAbsolutePath());
	}

	@Test
	void essentialExpenseSnapshotEntitiesAreDiscoverable() throws ClassNotFoundException {
		assertEntityClass("EssentialExpenseSnapshotWeek", "essential_expense_snapshot_week");
		assertEntityClass("EssentialExpenseSnapshotWeekCategory", "essential_expense_snapshot_week_category");
	}

	@Test
	void essentialExpenseSnapshotRepositoriesAreRegistered() {
		assertTrue(applicationContext.containsBeanDefinition("essentialExpenseSnapshotWeekRepository"));
		assertTrue(applicationContext.containsBeanDefinition("essentialExpenseSnapshotWeekCategoryRepository"));
	}

	@Test
	void essentialExpenseSnapshotRelationshipsUseExpectedForeignKeys() throws NoSuchFieldException {
		assertManyToOneField("EssentialExpenseSnapshotWeek", "user", User.class, "users_id");
		assertManyToOneField("EssentialExpenseSnapshotWeekCategory", "snapshotWeek",
			loadClass("EssentialExpenseSnapshotWeek"), "snapshot_week_id");
		assertManyToOneField("EssentialExpenseSnapshotWeekCategory", "category", Category.class, "category_id");
	}

	@Test
	void snapshotCategoryUsesBudgetExpenseScopeEnum() throws Exception {
		Class<?> entityClass = loadClass("EssentialExpenseSnapshotWeekCategory");
		Field field = entityClass.getDeclaredField("expenseScope");
		assertEquals(ExpenseScope.class, field.getType());
	}

	private void assertEntityClass(String simpleName, String tableName) throws ClassNotFoundException {
		Class<?> entityClass = loadClass(simpleName);
		assertNotNull(entityClass.getAnnotation(Entity.class));
		Table table = entityClass.getAnnotation(Table.class);
		assertNotNull(table);
		assertEquals(tableName, table.name());
	}

	private void assertManyToOneField(
		String ownerSimpleName,
		String fieldName,
		Class<?> fieldType,
		String joinColumnName
	) throws NoSuchFieldException {
		Class<?> ownerType = loadClass(ownerSimpleName);
		Field field = ownerType.getDeclaredField(fieldName);
		assertEquals(fieldType, field.getType());
		assertNotNull(field.getAnnotation(ManyToOne.class));
		JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
		assertNotNull(joinColumn);
		assertEquals(joinColumnName, joinColumn.name());
	}

	private Class<?> loadClass(String simpleName) {
		try {
			return Class.forName(ENTITY_PACKAGE + simpleName);
		} catch (ClassNotFoundException exception) {
			throw new AssertionError(exception);
		}
	}
}

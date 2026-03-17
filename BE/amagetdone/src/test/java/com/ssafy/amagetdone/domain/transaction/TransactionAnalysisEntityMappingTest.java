package com.ssafy.amagetdone.domain.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Path;

import com.ssafy.amagetdone.global.config.EnvLoader;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionOverride;

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
class TransactionAnalysisEntityMappingTest {

	private static final String ENTITY_PACKAGE = "com.ssafy.amagetdone.domain.transaction.model.entity.";

	@Autowired
	private ApplicationContext applicationContext;

	@BeforeAll
	static void loadEnv() {
		EnvLoader.loadRequiredEnv(Path.of("").toAbsolutePath());
	}

	@Test
	void transactionAnalysisEntitiesAreDiscoverable() throws ClassNotFoundException {
		assertEntityClass("TransactionAnalysisMetadata", "transaction_analysis_metadata");
		assertEntityClass("TransactionAnalysisResult", "transaction_analysis_result");
	}

	@Test
	void transactionAnalysisRepositoriesAreRegistered() {
		assertTrue(applicationContext.containsBeanDefinition("transactionAnalysisMetadataRepository"));
		assertTrue(applicationContext.containsBeanDefinition("transactionAnalysisResultRepository"));
	}

	@Test
	void transactionAnalysisRelationshipsUseExpectedForeignKeys() throws NoSuchFieldException {
		assertManyToOneField("TransactionAnalysisMetadata", "user", User.class, "users_id");
		assertManyToOneField("TransactionAnalysisResult", "transaction", TransactionOverride.class, "transaction_override_id");
		assertManyToOneField("TransactionAnalysisResult", "category", Category.class, "category_id");
	}

	private void assertEntityClass(String simpleName, String tableName) throws ClassNotFoundException {
		Class<?> entityClass = Class.forName(ENTITY_PACKAGE + simpleName);
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
		Class<?> ownerType;
		try {
			ownerType = Class.forName(ENTITY_PACKAGE + ownerSimpleName);
		} catch (ClassNotFoundException exception) {
			throw new AssertionError(exception);
		}

		Field field = ownerType.getDeclaredField(fieldName);
		assertEquals(fieldType, field.getType());
		assertNotNull(field.getAnnotation(ManyToOne.class));
		JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
		assertNotNull(joinColumn);
		assertEquals(joinColumnName, joinColumn.name());
	}
}

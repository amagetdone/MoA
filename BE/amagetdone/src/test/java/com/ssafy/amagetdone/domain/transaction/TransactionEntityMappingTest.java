package com.ssafy.amagetdone.domain.transaction;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
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
class TransactionEntityMappingTest {

	private static final String BASE_PACKAGE = "com.ssafy.amagetdone.domain.transaction.model.entity.";
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
	void transactionEntitiesAreDiscoverable() throws ClassNotFoundException {
		Class<?> rootEntity = assertEntityClass("TransactionOverride", "transaction_override");
		Class<?> cardOverride = assertEntityClass("CardTransactionOverride", "card_transaction_override");
		Class<?> accountOverride = assertEntityClass("AccountTransactionOverride", "account_transaction_override");

		assertTrue(rootEntity.isAssignableFrom(cardOverride));
		assertTrue(rootEntity.isAssignableFrom(accountOverride));
	}

	@Test
	void transactionRepositoriesAreRegistered() {
		assertTrue(applicationContext.containsBeanDefinition("transactionOverrideRepository"));
		assertTrue(applicationContext.containsBeanDefinition("cardTransactionOverrideRepository"));
		assertTrue(applicationContext.containsBeanDefinition("accountTransactionOverrideRepository"));
	}

	private Class<?> assertEntityClass(String simpleName, String tableName) throws ClassNotFoundException {
		Class<?> entityClass = Class.forName(BASE_PACKAGE + simpleName);
		assertNotNull(entityClass.getAnnotation(Entity.class));
		Table table = entityClass.getAnnotation(Table.class);
		assertNotNull(table);
		assertTrue(tableName.equals(table.name()));
		return entityClass;
	}
}

package com.ssafy.amagetdone.domain.asset;

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
class AssetEntityMappingTest {

	private static final String BASE_PACKAGE = "com.ssafy.amagetdone.domain.asset.model.entity.";

	@Autowired
	private ApplicationContext applicationContext;

	@BeforeAll
	static void loadEnv() {
		EnvLoader.loadRequiredEnv(Path.of("").toAbsolutePath());
	}

	@Test
	void assetEntitiesAreDiscoverable() throws ClassNotFoundException {
		assertEntityClass("Account", "account");
		assertEntityClass("Card", "card");
		assertEntityClass("Pocket", "pocket");
		assertEntityClass("Category", "category");
		assertEntityClass("Merchant", "merchant");
	}

	@Test
	void assetRepositoriesAreRegistered() {
		assertTrue(applicationContext.containsBeanDefinition("accountRepository"));
		assertTrue(applicationContext.containsBeanDefinition("cardRepository"));
		assertTrue(applicationContext.containsBeanDefinition("pocketRepository"));
		assertTrue(applicationContext.containsBeanDefinition("categoryRepository"));
		assertTrue(applicationContext.containsBeanDefinition("merchantRepository"));
	}

	private void assertEntityClass(String simpleName, String tableName) throws ClassNotFoundException {
		Class<?> entityClass = Class.forName(BASE_PACKAGE + simpleName);
		assertNotNull(entityClass.getAnnotation(Entity.class));
		Table table = entityClass.getAnnotation(Table.class);
		assertNotNull(table);
		assertTrue(tableName.equals(table.name()));
	}
}

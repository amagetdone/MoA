package com.ssafy.amagetdone.domain.member;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import com.ssafy.amagetdone.config.EnvLoader;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@SpringBootTest
class MemberEntityMappingTest {

	private static final String BASE_PACKAGE = "com.ssafy.amagetdone.domain.member.model.entity.";

	@Autowired
	private ApplicationContext applicationContext;

	@BeforeAll
	static void loadEnv() {
		EnvLoader.loadRequiredEnv(Path.of("").toAbsolutePath());
	}

	@Test
	void memberEntitiesAreDiscoverable() throws ClassNotFoundException {
		assertEntityClass("User", "users");
		assertEntityClass("SocialAccount", "social_account");
		assertEntityClass("Setting", "setting");
		assertEntityClass("Agreement", "user_agreements");
		assertEntityClass("Policy", "policy");
	}

	@Test
	void memberRepositoriesAreRegistered() {
		assertTrue(applicationContext.containsBeanDefinition("userRepository"));
		assertTrue(applicationContext.containsBeanDefinition("socialAccountRepository"));
		assertTrue(applicationContext.containsBeanDefinition("settingRepository"));
		assertTrue(applicationContext.containsBeanDefinition("agreementRepository"));
		assertTrue(applicationContext.containsBeanDefinition("policyRepository"));
	}

	private void assertEntityClass(String simpleName, String tableName) throws ClassNotFoundException {
		Class<?> entityClass = Class.forName(BASE_PACKAGE + simpleName);
		assertNotNull(entityClass.getAnnotation(Entity.class));
		Table table = entityClass.getAnnotation(Table.class);
		assertNotNull(table);
		assertTrue(tableName.equals(table.name()));
	}
}

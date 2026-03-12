package com.ssafy.amagetdone.domain.member;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.LocalDate;

import com.ssafy.amagetdone.domain.member.model.entity.SocialAccount;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.SocialAccountRepository;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;
import com.ssafy.amagetdone.config.EnvLoader;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@SpringBootTest
@ActiveProfiles("test")
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

	@Test
	void userAndSocialAccountFollowSingleSocialAccountDesign() throws Exception {
		Field socialAccountField = User.class.getDeclaredField("socialAccount");
		assertNotNull(socialAccountField.getAnnotation(OneToOne.class));
		assertSame(SocialAccount.class, socialAccountField.getType());

		Field userField = SocialAccount.class.getDeclaredField("user");
		assertNotNull(userField.getAnnotation(OneToOne.class));

		JoinColumn joinColumn = userField.getAnnotation(JoinColumn.class);
		assertNotNull(joinColumn);
		assertTrue(joinColumn.unique());
		assertTrue("users_id".equals(joinColumn.name()));

		Table table = SocialAccount.class.getAnnotation(Table.class);
		assertNotNull(table);
		assertTrue(hasUniqueConstraint(table, "provider", "provider_uid"));
	}

	@Test
	void authRelatedMemberFieldsUseExpectedTypes() throws Exception {
		Field birthDateField = User.class.getDeclaredField("birthDate");
		assertSame(LocalDate.class, birthDateField.getType());

		Field hasFixedIncomeField = User.class.getDeclaredField("hasFixedIncome");
		assertSame(Boolean.class, hasFixedIncomeField.getType());

		Field providerField = SocialAccount.class.getDeclaredField("provider");
		assertSame(SocialProvider.class, providerField.getType());

		Enumerated enumerated = providerField.getAnnotation(Enumerated.class);
		assertNotNull(enumerated);
		assertSame(EnumType.STRING, enumerated.value());

		Field userKeyField = User.class.getDeclaredField("userKey");
		assertSame(String.class, userKeyField.getType());
		assertEquals("user_key", userKeyField.getAnnotation(jakarta.persistence.Column.class).name());
	}

	@Test
	void memberEntityIdsUseIdentityGeneration() throws Exception {
		assertIdentityId(User.class.getDeclaredField("id"));
		assertIdentityId(SocialAccount.class.getDeclaredField("id"));
	}

	@Test
	void socialAccountRepositorySupportsProviderBasedLookup() throws Exception {
		Method method = SocialAccountRepository.class.getMethod(
			"findByProviderAndProviderUid",
			SocialProvider.class,
			String.class
		);
		assertNotNull(method);
	}

	private void assertEntityClass(String simpleName, String tableName) throws ClassNotFoundException {
		Class<?> entityClass = Class.forName(BASE_PACKAGE + simpleName);
		assertNotNull(entityClass.getAnnotation(Entity.class));
		Table table = entityClass.getAnnotation(Table.class);
		assertNotNull(table);
		assertTrue(tableName.equals(table.name()));
	}

	private void assertIdentityId(Field field) {
		GeneratedValue generatedValue = field.getAnnotation(GeneratedValue.class);
		assertNotNull(generatedValue);
		assertSame(GenerationType.IDENTITY, generatedValue.strategy());
	}

	private boolean hasUniqueConstraint(Table table, String... columnNames) {
		for (UniqueConstraint uniqueConstraint : table.uniqueConstraints()) {
			if (java.util.Arrays.equals(columnNames, uniqueConstraint.columnNames())) {
				return true;
			}
		}
		return false;
	}
}

package com.ssafy.amagetdone.domain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.ssafy.amagetdone.domain.account.model.entity.Card;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.entity.Merchant;
import com.ssafy.amagetdone.domain.account.model.entity.Pocket;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetRollover;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialWeekAllocation;
import com.ssafy.amagetdone.domain.member.model.entity.Agreement;
import com.ssafy.amagetdone.domain.member.model.entity.Policy;
import com.ssafy.amagetdone.domain.member.model.entity.Setting;
import com.ssafy.amagetdone.domain.member.model.entity.SocialAccount;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionOverride;

import org.junit.jupiter.api.Test;

class EntityLombokAnnotationTest {

	private static final List<Class<?>> BUILDER_ENTITIES = List.of(
		Category.class,
		Merchant.class
	);

	private static final List<Class<?>> SETTER_REQUIRED_SUPER_BUILDER_ENTITIES = List.of(
		Pocket.class,
		BudgetAdjustment.class,
		BudgetMonth.class,
		BudgetRollover.class,
		BudgetWeek.class,
		EssentialExpenseMonth.class,
		EssentialWeekAllocation.class,
		Agreement.class,
		Policy.class,
		Setting.class,
		SocialAccount.class,
		User.class,
		Plan.class,
		PlanWeekAllocation.class,
		TransactionOverride.class,
		AccountTransactionOverride.class,
		CardTransactionOverride.class
	);

	private static final List<Class<?>> NO_SETTER_SUPER_BUILDER_ENTITIES = List.of(
		Card.class
	);

	@Test
	void allEntitiesExposeRequiredReadAndConstructionAnnotations() throws IOException {
		List<Class<?>> allEntities = List.copyOf(BUILDER_ENTITIES);
		allEntities = new java.util.ArrayList<>(allEntities);
		allEntities.addAll(SETTER_REQUIRED_SUPER_BUILDER_ENTITIES);
		allEntities.addAll(NO_SETTER_SUPER_BUILDER_ENTITIES);

		for (Class<?> entityClass : allEntities) {
			assertSourceContains(entityClass, "@Getter");
			assertSourceContains(entityClass, "@NoArgsConstructor");
			assertSourceContains(entityClass, "@AllArgsConstructor");
		}
	}

	@Test
	void setterManagedEntitiesExposeSetterAnnotation() throws IOException {
		List<Class<?>> setterManagedEntities = List.copyOf(BUILDER_ENTITIES);
		setterManagedEntities = new java.util.ArrayList<>(setterManagedEntities);
		setterManagedEntities.addAll(SETTER_REQUIRED_SUPER_BUILDER_ENTITIES);

		for (Class<?> entityClass : setterManagedEntities) {
			assertSourceContains(entityClass, "@Setter");
		}
	}

	@Test
	void builderEntitiesUseExpectedBuilderAnnotation() throws IOException {
		for (Class<?> entityClass : BUILDER_ENTITIES) {
			assertSourceContains(entityClass, "@Builder");
		}

		List<Class<?>> superBuilderEntities = List.copyOf(SETTER_REQUIRED_SUPER_BUILDER_ENTITIES);
		superBuilderEntities = new java.util.ArrayList<>(superBuilderEntities);
		superBuilderEntities.addAll(NO_SETTER_SUPER_BUILDER_ENTITIES);

		for (Class<?> entityClass : superBuilderEntities) {
			assertSourceContains(entityClass, "@SuperBuilder");
		}
	}

	@Test
	void representativeEntitiesExposeBuilderMethod() throws NoSuchMethodException {
		assertBuilderMethodExists(Category.class);
		assertBuilderMethodExists(User.class);
		assertBuilderMethodExists(CardTransactionOverride.class);
	}

	private void assertSourceContains(Class<?> targetClass, String annotationText) throws IOException {
		String source = Files.readString(sourcePath(targetClass));
		assertTrue(source.contains(annotationText), () -> targetClass.getSimpleName() + " is missing " + annotationText);
	}

	private void assertBuilderMethodExists(Class<?> targetClass) throws NoSuchMethodException {
		Method builderMethod = targetClass.getDeclaredMethod("builder");

		assertTrue(java.lang.reflect.Modifier.isStatic(builderMethod.getModifiers()));
	}

	private Path sourcePath(Class<?> targetClass) {
		return Path.of("src", "main", "java")
			.resolve(targetClass.getPackageName().replace('.', java.io.File.separatorChar))
			.resolve(targetClass.getSimpleName() + ".java");
	}
}

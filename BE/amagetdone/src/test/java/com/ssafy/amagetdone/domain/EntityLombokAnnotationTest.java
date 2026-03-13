package com.ssafy.amagetdone.domain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.ssafy.amagetdone.domain.asset.model.entity.Account;
import com.ssafy.amagetdone.domain.asset.model.entity.Card;
import com.ssafy.amagetdone.domain.asset.model.entity.Category;
import com.ssafy.amagetdone.domain.asset.model.entity.Merchant;
import com.ssafy.amagetdone.domain.asset.model.entity.Pocket;
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

	private static final List<Class<?>> SUPER_BUILDER_ENTITIES = List.of(
		Account.class,
		Card.class,
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

	@Test
	void allEntitiesExposeRequiredLombokAnnotations() throws IOException {
		List<Class<?>> allEntities = List.copyOf(BUILDER_ENTITIES);
		allEntities = new java.util.ArrayList<>(allEntities);
		allEntities.addAll(SUPER_BUILDER_ENTITIES);

		for (Class<?> entityClass : allEntities) {
			assertSourceContains(entityClass, "@Getter");
			assertSourceContains(entityClass, "@Setter");
			assertSourceContains(entityClass, "@NoArgsConstructor");
			assertSourceContains(entityClass, "@AllArgsConstructor");
		}
	}

	@Test
	void builderEntitiesUseExpectedBuilderAnnotation() throws IOException {
		for (Class<?> entityClass : BUILDER_ENTITIES) {
			assertSourceContains(entityClass, "@Builder");
		}

		for (Class<?> entityClass : SUPER_BUILDER_ENTITIES) {
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

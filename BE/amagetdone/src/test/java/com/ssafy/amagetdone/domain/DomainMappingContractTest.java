package com.ssafy.amagetdone.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;

import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Card;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.entity.Merchant;
import com.ssafy.amagetdone.domain.account.model.entity.Pocket;
import com.ssafy.amagetdone.domain.accountverification.model.entity.AccountVerification;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetRollover;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeekCategory;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;
import com.ssafy.amagetdone.domain.member.model.entity.SocialAccount;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanAiPipelineStatus;
import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisMetadata;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisResult;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionOverride;

import org.hibernate.annotations.ColumnDefault;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

class DomainMappingContractTest {

	@Test
	void memberSchemaContractsRemainStable() throws Exception {
		assertTableName(User.class, "users");
		assertTableName(SocialAccount.class, "social_account");
		assertColumnName(User.class, "hasFixedIncome", "has_fixed_income");
		assertColumnName(User.class, "userKey", "user_key");
		assertGeneratedStrategy(User.class, "id", GenerationType.IDENTITY);
		assertGeneratedStrategy(SocialAccount.class, "id", GenerationType.IDENTITY);

		Field userField = SocialAccount.class.getDeclaredField("user");
		JoinColumn joinColumn = userField.getAnnotation(JoinColumn.class);
		assertNotNull(joinColumn);
		assertEquals("users_id", joinColumn.name());
		assertTrue(joinColumn.unique());

		Table table = SocialAccount.class.getAnnotation(Table.class);
		assertNotNull(table);
		assertTrue(hasUniqueConstraint(table, "provider", "provider_uid"));
	}

	@Test
	void representativeDefaultsRemainStable() throws Exception {
		assertColumnDefault(Account.class, "active", "TRUE");
		assertColumnDefault(Account.class, "verified", "FALSE");
		assertColumnDefault(AccountVerification.class, "status", "'REQUESTED'");
		assertColumnDefault(User.class, "status", "'ACTIVE'");
		assertColumnDefault(BudgetMonth.class, "status", "'OPEN'");
		assertColumnDefault(BudgetWeek.class, "status", "'PLANNED'");
		assertColumnDefault(BudgetWeek.class, "periodDays", "7");
		assertColumnDefault(BudgetRollover.class, "status", "'APPLIED'");
		assertColumnDefault(EssentialExpenseMonth.class, "sourceProvider", "'ANALYZER'");
		assertColumnDefault(CardTransactionOverride.class, "billStatementsStatus", "'미결제'");
	}

	@Test
	void criticalEnumMappingsRemainStringBased() throws Exception {
		assertEnumerated(User.class, "status", EnumType.STRING);
		assertEnumerated(BudgetMonth.class, "status", EnumType.STRING);
		assertEnumerated(BudgetWeek.class, "status", EnumType.STRING);
		assertEnumerated(EssentialExpenseMonth.class, "sourceProvider", EnumType.STRING);
		assertEnumerated(TransactionAnalysisResult.class, "excludedReason", EnumType.STRING);
		assertEnumerated(TransactionAnalysisResult.class, "expenseScope", EnumType.STRING);
		assertEnumerated(EssentialExpenseSnapshotWeekCategory.class, "expenseScope", EnumType.STRING);
	}

	@Test
	void planEntitiesKeepKeyIdAndBuilderDefaults() throws Exception {
		assertTableName(Plan.class, "plan");
		assertTableName(PlanWeekAllocation.class, "plan_week_allocation");
		assertGeneratedStrategy(PlanWeekAllocation.class, "id", GenerationType.SEQUENCE);

		SequenceGenerator sequenceGenerator = PlanWeekAllocation.class.getDeclaredField("id")
			.getAnnotation(SequenceGenerator.class);
		assertNotNull(sequenceGenerator);
		assertEquals("plan_week_allocation_seq", sequenceGenerator.sequenceName());

		Plan plan = Plan.builder().build();
		assertSame(PlanAiPipelineStatus.PENDING, plan.getAiPipelineStatus());
		assertEquals(0, plan.getAiRetryCount());
	}

	@Test
	void snapshotRelationshipsRemainStable() throws Exception {
		assertTableName(EssentialExpenseSnapshotWeek.class, "essential_expense_snapshot_week");
		assertTableName(EssentialExpenseSnapshotWeekCategory.class, "essential_expense_snapshot_week_category");
		assertManyToOneField(EssentialExpenseSnapshotWeek.class, "user", User.class, "users_id");
		assertManyToOneField(
			EssentialExpenseSnapshotWeekCategory.class,
			"snapshotWeek",
			EssentialExpenseSnapshotWeek.class,
			"snapshot_week_id"
		);
		assertManyToOneField(EssentialExpenseSnapshotWeekCategory.class, "category", Category.class, "category_id");
		assertEquals(ExpenseScope.class, EssentialExpenseSnapshotWeekCategory.class.getDeclaredField("expenseScope").getType());
	}

	@Test
	void transactionRelationshipsRemainStable() throws Exception {
		assertTableName(TransactionOverride.class, "transaction_override");
		assertTableName(CardTransactionOverride.class, "card_transaction_override");
		assertTableName(AccountTransactionOverride.class, "account_transaction_override");
		assertTableName(TransactionAnalysisMetadata.class, "transaction_analysis_metadata");
		assertTableName(TransactionAnalysisResult.class, "transaction_analysis_result");
		assertColumnName(TransactionAnalysisMetadata.class, "requestedMonths", "requested_months");
		assertManyToOneField(Pocket.class, "budgetWeek", BudgetWeek.class, "budget_week_id");
		assertManyToOneField(TransactionOverride.class, "category", Category.class, "category_id");
		assertManyToOneField(CardTransactionOverride.class, "merchant", Merchant.class, "merchant_id");
		assertManyToOneField(CardTransactionOverride.class, "card", Card.class, "cardUniqueNo");
		assertManyToOneField(AccountTransactionOverride.class, "account", Account.class, "account_id");
		assertManyToOneField(TransactionAnalysisMetadata.class, "user", User.class, "users_id");
		assertManyToOneField(TransactionAnalysisResult.class, "transaction", TransactionOverride.class, "transaction_override_id");
		assertManyToOneField(TransactionAnalysisResult.class, "category", Category.class, "category_id");
	}

	private void assertTableName(Class<?> type, String expected) {
		Table table = type.getAnnotation(Table.class);
		assertNotNull(table, () -> type.getSimpleName() + " must declare @Table");
		assertEquals(expected, table.name());
	}

	private void assertColumnName(Class<?> type, String fieldName, String expected) throws Exception {
		Field field = type.getDeclaredField(fieldName);
		Column column = field.getAnnotation(Column.class);
		assertNotNull(column, () -> type.getSimpleName() + "." + fieldName + " must declare @Column");
		assertEquals(expected, column.name());
	}

	private void assertColumnDefault(Class<?> type, String fieldName, String expected) throws Exception {
		Field field = type.getDeclaredField(fieldName);
		ColumnDefault columnDefault = field.getAnnotation(ColumnDefault.class);
		assertNotNull(columnDefault, () -> type.getSimpleName() + "." + fieldName + " must declare @ColumnDefault");
		assertTrue(expected.equals(columnDefault.value()) || expected.equalsIgnoreCase(columnDefault.value()));
	}

	private void assertGeneratedStrategy(Class<?> type, String fieldName, GenerationType expected) throws Exception {
		Field field = type.getDeclaredField(fieldName);
		GeneratedValue generatedValue = field.getAnnotation(GeneratedValue.class);
		assertNotNull(generatedValue, () -> type.getSimpleName() + "." + fieldName + " must declare @GeneratedValue");
		assertEquals(expected, generatedValue.strategy());
	}

	private void assertEnumerated(Class<?> type, String fieldName, EnumType expected) throws Exception {
		Field field = type.getDeclaredField(fieldName);
		Enumerated enumerated = field.getAnnotation(Enumerated.class);
		assertNotNull(enumerated, () -> type.getSimpleName() + "." + fieldName + " must declare @Enumerated");
		assertEquals(expected, enumerated.value());
	}

	private void assertManyToOneField(
		Class<?> ownerType,
		String fieldName,
		Class<?> fieldType,
		String joinColumnName
	) throws Exception {
		Field field = ownerType.getDeclaredField(fieldName);
		assertEquals(fieldType, field.getType());
		assertNotNull(field.getAnnotation(ManyToOne.class));
		JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
		assertNotNull(joinColumn);
		assertEquals(joinColumnName, joinColumn.name());
	}

	private boolean hasUniqueConstraint(Table table, String... expectedColumnNames) {
		return Arrays.stream(table.uniqueConstraints())
			.map(UniqueConstraint::columnNames)
			.anyMatch(columnNames -> Arrays.equals(expectedColumnNames, columnNames));
	}
}

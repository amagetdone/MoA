package com.ssafy.amagetdone.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Card;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetRollover;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialWeekAllocation;
import com.ssafy.amagetdone.domain.member.model.entity.Setting;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.global.entity.BaseEntity;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;

import org.hibernate.annotations.ColumnDefault;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;

class ErdSchemaAlignmentTest {

	@Test
	void correctedColumnNamesMatchDirectErd() throws Exception {
		assertColumnName(User.class, "hasFixedIncome", "has_fixed_income");
		assertColumnName(Card.class, "cardIssuerName", "cardIssuerName");
	}

	@Test
	void postgresCompatibleMappingsAvoidRawEnumDefinitions() throws Exception {
		assertNoRawEnumColumnDefinition(User.class, "hasFixedIncome");
		assertNoRawEnumColumnDefinition(User.class, "status");
		assertEnumerated(Plan.class, "status", true);
		assertEnumerated(BudgetMonth.class, "status", true);
		assertEnumerated(BudgetWeek.class, "status", true);
		assertEnumerated(BudgetAdjustment.class, "adjustmentType", true);
		assertEnumerated(BudgetRollover.class, "status", true);
		assertEnumerated(EssentialExpenseMonth.class, "sourceProvider", true);
	}

	@Test
	void booleanColumnsUsePostgresBooleanDefaults() throws Exception {
		assertColumnDefault(BaseEntity.class, "deleted", "FALSE");
		assertColumnDefault(Account.class, "verified", "FALSE");
		assertColumnDefault(Setting.class, "alarm", "FALSE");
	}

	@Test
	void erdDefaultsAreDeclaredOnEntityMappings() throws Exception {
		assertColumnDefault(Account.class, "verified", "FALSE");
		assertColumnDefault(Setting.class, "alarm", "FALSE");
		assertColumnDefault(User.class, "status", "'ACTIVE'");
		assertColumnDefault(Plan.class, "savedAmount", "0");
		assertColumnDefault(Plan.class, "status", "'ACTIVE'");
		assertColumnDefault(Plan.class, "autoAllocateEnabled", "TRUE");
		assertColumnDefault(PlanWeekAllocation.class, "allocationType", "'AUTO'");
		assertColumnDefault(PlanWeekAllocation.class, "plannedAmount", "0");
		assertColumnDefault(PlanWeekAllocation.class, "finalAmount", "0");
		assertColumnDefault(BudgetMonth.class, "status", "'OPEN'");
		assertColumnDefault(BudgetMonth.class, "totalIncomeAmount", "0");
		assertColumnDefault(BudgetMonth.class, "totalFixedEssentialAmount", "0");
		assertColumnDefault(BudgetMonth.class, "totalVariableEssentialAmount", "0");
		assertColumnDefault(BudgetWeek.class, "periodDays", "7");
		assertColumnDefault(BudgetWeek.class, "partial", "FALSE");
		assertColumnDefault(BudgetWeek.class, "status", "'PLANNED'");
		assertColumnDefault(BudgetWeek.class, "plannedIncomeAmount", "0");
		assertColumnDefault(BudgetWeek.class, "plannedEssentialAmount", "0");
		assertColumnDefault(BudgetWeek.class, "plannedSavingAmount", "0");
		assertColumnDefault(BudgetWeek.class, "rolloverInAmount", "0");
		assertColumnDefault(BudgetWeek.class, "manualAdjustmentAmount", "0");
		assertColumnDefault(BudgetWeek.class, "finalFreeAmount", "0");
		assertColumnDefault(BudgetWeek.class, "actualSpentAmount", "0");
		assertColumnDefault(EssentialExpenseMonth.class, "sourceProvider", "'ANALYZER'");
		assertColumnDefault(EssentialWeekAllocation.class, "plannedAmount", "0");
		assertColumnDefault(EssentialWeekAllocation.class, "actualSpentAmount", "0");
		assertColumnDefault(BudgetRollover.class, "status", "'APPLIED'");
		assertColumnDefault(CardTransactionOverride.class, "billStatementsStatus", "'미결제'");
	}

	private void assertColumnName(Class<?> type, String fieldName, String expectedName) throws Exception {
		Field field = type.getDeclaredField(fieldName);
		Column column = field.getAnnotation(Column.class);
		assertNotNull(column, () -> type.getSimpleName() + "." + fieldName + " must declare @Column");
		assertEquals(expectedName, column.name());
	}

	private void assertNoRawEnumColumnDefinition(Class<?> type, String fieldName) throws Exception {
		Field field = type.getDeclaredField(fieldName);
		Column column = field.getAnnotation(Column.class);
		assertNotNull(column, () -> type.getSimpleName() + "." + fieldName + " must declare @Column");
		assertFalse("ENUM".equalsIgnoreCase(column.columnDefinition().trim()));
	}

	private void assertEnumerated(Class<?> type, String fieldName, boolean expected) throws Exception {
		Field field = type.getDeclaredField(fieldName);
		if (expected) {
			assertNotNull(field.getAnnotation(Enumerated.class));
			return;
		}
		assertFalse(field.isAnnotationPresent(Enumerated.class));
	}

	private void assertColumnDefault(Class<?> type, String fieldName, String expected) throws Exception {
		Field field = type.getDeclaredField(fieldName);
		ColumnDefault columnDefault = field.getAnnotation(ColumnDefault.class);
		assertNotNull(columnDefault, () -> type.getSimpleName() + "." + fieldName + " must declare @ColumnDefault");
		assertTrue(expected.equals(columnDefault.value()) || expected.equalsIgnoreCase(columnDefault.value()));
	}
}

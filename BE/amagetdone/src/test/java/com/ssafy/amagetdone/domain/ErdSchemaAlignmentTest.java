package com.ssafy.amagetdone.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import com.ssafy.amagetdone.domain.asset.model.entity.Account;
import com.ssafy.amagetdone.domain.asset.model.entity.Card;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetRollover;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialWeekAllocation;
import com.ssafy.amagetdone.domain.member.model.entity.Setting;
import com.ssafy.amagetdone.domain.member.model.entity.User;
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
		assertColumnName(User.class, "jobStatus", "job_status");
		assertColumnName(Card.class, "cardIssuerName", "cardIssuerName");
	}

	@Test
	void onlyRealEnumColumnsUseEnumerated() throws Exception {
		assertColumnDefinitionContains(User.class, "jobStatus", "ENUM");
		assertColumnDefinitionContains(User.class, "status", "ENUM");
		assertEnumerated(PlanWeekAllocation.class, "allocationType", false);
		assertEnumerated(EssentialExpenseMonth.class, "expenseScope", false);
	}

	@Test
	void erdDefaultsAreDeclaredOnEntityMappings() throws Exception {
		assertColumnDefault(Account.class, "verified", "0");
		assertColumnDefault(Setting.class, "alarm", "0");
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

	private void assertEnumerated(Class<?> type, String fieldName, boolean expected) throws Exception {
		Field field = type.getDeclaredField(fieldName);
		if (expected) {
			assertNotNull(field.getAnnotation(Enumerated.class));
			return;
		}
		assertFalse(field.isAnnotationPresent(Enumerated.class));
	}

	private void assertColumnDefinitionContains(Class<?> type, String fieldName, String expectedFragment) throws Exception {
		Field field = type.getDeclaredField(fieldName);
		Column column = field.getAnnotation(Column.class);
		assertNotNull(column, () -> type.getSimpleName() + "." + fieldName + " must declare @Column");
		assertTrue(column.columnDefinition().toUpperCase().contains(expectedFragment.toUpperCase()));
	}

	private void assertColumnDefault(Class<?> type, String fieldName, String expected) throws Exception {
		Field field = type.getDeclaredField(fieldName);
		ColumnDefault columnDefault = field.getAnnotation(ColumnDefault.class);
		assertNotNull(columnDefault, () -> type.getSimpleName() + "." + fieldName + " must declare @ColumnDefault");
		assertTrue(expected.equals(columnDefault.value()) || expected.equalsIgnoreCase(columnDefault.value()));
	}
}

package com.ssafy.amagetdone.domain.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;

import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Card;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.entity.Merchant;
import com.ssafy.amagetdone.domain.account.model.entity.Pocket;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionOverride;

import org.junit.jupiter.api.Test;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

class FkRelationshipMappingTest {

	@Test
	void pocketUsesBudgetWeekRelationship() throws NoSuchFieldException {
		assertManyToOneField(Pocket.class, "budgetWeek", BudgetWeek.class, "budget_week_id");
	}

	@Test
	void transactionOverrideUsesCategoryRelationship() throws NoSuchFieldException {
		assertManyToOneField(TransactionOverride.class, "category", Category.class, "category_id");
	}

	@Test
	void cardTransactionOverrideUsesMerchantAndCardRelationships() throws NoSuchFieldException {
		assertManyToOneField(CardTransactionOverride.class, "merchant", Merchant.class, "merchant_id");
		assertManyToOneField(CardTransactionOverride.class, "card", Card.class, "cardUniqueNo");
	}

	@Test
	void accountTransactionOverrideUsesAccountRelationship() throws NoSuchFieldException {
		assertManyToOneField(AccountTransactionOverride.class, "account", Account.class, "account_id");
	}

	private void assertManyToOneField(
		Class<?> ownerType,
		String fieldName,
		Class<?> fieldType,
		String joinColumnName
	) throws NoSuchFieldException {
		Field field = ownerType.getDeclaredField(fieldName);
		assertEquals(fieldType, field.getType());
		assertNotNull(field.getAnnotation(ManyToOne.class));
		JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
		assertNotNull(joinColumn);
		assertEquals(joinColumnName, joinColumn.name());
	}
}

package com.ssafy.amagetdone.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.ssafy.amagetdone.global.entity.BaseEntity;

import org.junit.jupiter.api.Test;

class BaseEntityScopeTest {

	@Test
	void operationalEntitiesExtendBaseEntity() {
		assertExtendsBaseEntity(User.class);
		assertExtendsBaseEntity(SocialAccount.class);
		assertExtendsBaseEntity(Setting.class);
		assertExtendsBaseEntity(Agreement.class);
		assertExtendsBaseEntity(Policy.class);
		assertExtendsBaseEntity(Account.class);
		assertExtendsBaseEntity(Card.class);
		assertExtendsBaseEntity(Pocket.class);
		assertExtendsBaseEntity(TransactionOverride.class);
		assertExtendsBaseEntity(CardTransactionOverride.class);
		assertExtendsBaseEntity(AccountTransactionOverride.class);
		assertExtendsBaseEntity(Plan.class);
		assertExtendsBaseEntity(PlanWeekAllocation.class);
		assertExtendsBaseEntity(BudgetMonth.class);
		assertExtendsBaseEntity(EssentialExpenseMonth.class);
		assertExtendsBaseEntity(BudgetWeek.class);
		assertExtendsBaseEntity(EssentialWeekAllocation.class);
		assertExtendsBaseEntity(BudgetAdjustment.class);
		assertExtendsBaseEntity(BudgetRollover.class);
	}

	@Test
	void masterEntitiesDoNotExtendBaseEntity() {
		assertEquals(Object.class, Category.class.getSuperclass());
		assertEquals(Object.class, Merchant.class.getSuperclass());
	}

	private void assertExtendsBaseEntity(Class<?> entityClass) {
		assertTrue(BaseEntity.class.isAssignableFrom(entityClass));
	}
}

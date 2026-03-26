package com.ssafy.amagetdone.domain.budget.model.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetMonth;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetMonthStatus;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import jakarta.persistence.EntityManager;

@DataJpaTest
class BudgetMonthRepositoryTest {

	@Autowired
	private BudgetMonthRepository budgetMonthRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void findFirstByUserIdAndBaseDateOrderByIdDescReturnsLatestRowWhenDuplicatesExist() {
		User user = persistUser("budget-month-duplicate@test.com");
		LocalDate baseDate = LocalDate.of(2026, 3, 1);

		BudgetMonth older = budgetMonthRepository.save(BudgetMonth.builder()
			.user(user)
			.baseDate(baseDate)
			.status(BudgetMonthStatus.OPEN)
			.totalIncomeAmount(1_000_000L)
			.build());
		BudgetMonth newer = budgetMonthRepository.save(BudgetMonth.builder()
			.user(user)
			.baseDate(baseDate)
			.status(BudgetMonthStatus.OPEN)
			.totalIncomeAmount(2_000_000L)
			.build());

		entityManager.flush();
		entityManager.clear();

		BudgetMonth result = budgetMonthRepository.findFirstByUser_IdAndBaseDateOrderByIdDesc(user.getId(), baseDate)
			.orElseThrow();

		assertThat(result.getId()).isEqualTo(newer.getId());
		assertThat(result.getId()).isNotEqualTo(older.getId());
		assertThat(result.getTotalIncomeAmount()).isEqualTo(2_000_000L);
	}

	private User persistUser(String email) {
		User user = User.createPendingSocialUser();
		user.applyTestSocialProfile("tester", email);
		user.activate();
		entityManager.persist(user);
		return user;
	}
}

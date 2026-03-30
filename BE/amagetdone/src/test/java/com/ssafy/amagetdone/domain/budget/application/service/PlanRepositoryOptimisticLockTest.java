package com.ssafy.amagetdone.domain.budget.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.LocalDate;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
@DataJpaTest
class PlanRepositoryOptimisticLockTest {

	@jakarta.annotation.Resource
	private EntityManagerFactory entityManagerFactory;

	@Test
	void stalePlanSaveFailsAfterConcurrentAiUpdate() {
		Integer planId = persistPlan();
		Plan stalePlan = loadDetachedPlan(planId);

		updatePlanAiState(planId);
		stalePlan.setSavedAmount(50_000L);

		Throwable thrown = catchThrowable(() -> mergeDetachedPlan(stalePlan));

		assertThat(thrown).isInstanceOf(OptimisticLockException.class);
	}

	private Integer persistPlan() {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			entityManager.getTransaction().begin();
			User user = User.createPendingSocialUser();
			user.applyTestSocialProfile("tester", "tester@example.com");
			user.activate();
			entityManager.persist(user);

			Plan plan = Plan.create(
				user,
				"Travel Fund",
				"Summer travel savings",
				1_000_000L,
				LocalDate.of(2026, 3, 1),
				LocalDate.of(2026, 6, 30),
				1
			);
			plan.activate();
			entityManager.persist(plan);
			entityManager.getTransaction().commit();
			return plan.getId();
		} finally {
			entityManager.close();
		}
	}

	private Plan loadDetachedPlan(Integer planId) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			entityManager.getTransaction().begin();
			Plan plan = entityManager.find(Plan.class, planId);
			entityManager.getTransaction().commit();
			return plan;
		} finally {
			entityManager.close();
		}
	}

	private void updatePlanAiState(Integer planId) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			entityManager.getTransaction().begin();
			Plan plan = entityManager.find(Plan.class, planId);
			plan.updatePlanUrl("plan/generated.png");
			plan.updateVideoUrl("https://cdn.example.com/plan.mp4");
			plan.markAiSucceeded("https://cdn.example.com/plan.mp4");
			entityManager.getTransaction().commit();
		} finally {
			entityManager.close();
		}
	}

	private void mergeDetachedPlan(Plan stalePlan) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(stalePlan);
			entityManager.flush();
			entityManager.getTransaction().commit();
		} catch (RuntimeException exception) {
			if (entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			throw exception;
		} finally {
			entityManager.close();
		}
	}
}

package com.ssafy.amagetdone.domain.plan.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanListItemResponse;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;

@DataJpaTest
@Import(ListPlansUseCase.class)
class ListPlansUseCaseDataJpaTest {

	@Autowired
	private ListPlansUseCase listPlansUseCase;

	@Autowired
	private EntityManager entityManager;

	@Test
	void executeIncludesWaitingPlansInList() {
		User user = persistUser("plan-list-user@example.com");
		LocalDate today = LocalDate.now();

		Plan waitingPlan = persistPlan(
			user,
			"Future plan",
			PlanStatus.WAITING,
			1,
			today.plusDays(7),
			today.plusDays(35)
		);
		Plan activePlan = persistPlan(
			user,
			"Current plan",
			PlanStatus.ACTIVE,
			2,
			today,
			today.plusDays(21)
		);
		persistPlan(
			user,
			"Completed plan",
			PlanStatus.COMPLETED,
			3,
			today.minusDays(30),
			today.minusDays(1)
		);

		entityManager.flush();
		entityManager.clear();

		List<PlanListItemResponse> response = listPlansUseCase.execute(user.getId());

		assertThat(response)
			.extracting(PlanListItemResponse::planId, PlanListItemResponse::title)
			.containsExactly(
				org.assertj.core.groups.Tuple.tuple(waitingPlan.getId().longValue(), "Future plan"),
				org.assertj.core.groups.Tuple.tuple(activePlan.getId().longValue(), "Current plan")
			);
	}

	private User persistUser(String email) {
		User user = User.createPendingSocialUser();
		user.applyTestSocialProfile("tester", email);
		user.activate();
		entityManager.persist(user);
		return user;
	}

	private Plan persistPlan(
		User user,
		String title,
		PlanStatus status,
		int priority,
		LocalDate startDate,
		LocalDate endDate
	) {
		Plan plan = Plan.create(
			user,
			title,
			title + " description",
			1_000_000L,
			startDate,
			endDate,
			priority
		);
		plan.setStatus(status);
		entityManager.persist(plan);
		return plan;
	}
}

package com.ssafy.amagetdone.domain.plan.model.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanWeekAllocationRepository extends JpaRepository<PlanWeekAllocation, Integer> {

	@Modifying
	@Query("DELETE FROM PlanWeekAllocation a WHERE a.plan.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Integer userId);

	List<PlanWeekAllocation> findByBudgetWeek(BudgetWeek budgetWeek);

	List<PlanWeekAllocation> findByBudgetWeekAndActualAmountIsNull(BudgetWeek budgetWeek);

	List<PlanWeekAllocation> findAllByBudgetWeekIn(List<BudgetWeek> budgetWeeks);

	List<PlanWeekAllocation> findAllByPlanIdOrderByBudgetWeekWeekSequenceAsc(Integer planId);

	List<PlanWeekAllocation> findAllByPlanIdInAndBudgetWeekWeekStartDateBefore(
		Collection<Integer> planIds,
		LocalDate weekStartDate
	);

	void deleteByPlanUserId(Integer userId);
}

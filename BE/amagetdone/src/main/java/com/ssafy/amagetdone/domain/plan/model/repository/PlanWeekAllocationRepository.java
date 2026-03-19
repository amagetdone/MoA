package com.ssafy.amagetdone.domain.plan.model.repository;

import java.util.List;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanWeekAllocationRepository extends JpaRepository<PlanWeekAllocation, Integer> {

	List<PlanWeekAllocation> findByBudgetWeek(BudgetWeek budgetWeek);

	List<PlanWeekAllocation> findAllByPlanIdOrderByBudgetWeekWeekSequenceAsc(Integer planId);

	void deleteByPlanUserId(Integer userId);
}

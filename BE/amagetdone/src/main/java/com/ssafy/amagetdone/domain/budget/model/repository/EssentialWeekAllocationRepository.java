package com.ssafy.amagetdone.domain.budget.model.repository;

import java.util.List;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialWeekAllocation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EssentialWeekAllocationRepository extends JpaRepository<EssentialWeekAllocation, Integer> {

	List<EssentialWeekAllocation> findByBudgetWeek(BudgetWeek budgetWeek);

	List<EssentialWeekAllocation> findAllByBudgetWeekIn(List<BudgetWeek> budgetWeeks);
}

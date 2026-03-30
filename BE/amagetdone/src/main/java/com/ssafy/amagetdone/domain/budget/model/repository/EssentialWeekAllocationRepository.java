package com.ssafy.amagetdone.domain.budget.model.repository;

import java.util.List;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialWeekAllocation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EssentialWeekAllocationRepository extends JpaRepository<EssentialWeekAllocation, Integer> {

	@Modifying
	@Query("DELETE FROM EssentialWeekAllocation a WHERE a.budgetWeek.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Integer userId);

	List<EssentialWeekAllocation> findByBudgetWeek(BudgetWeek budgetWeek);

	List<EssentialWeekAllocation> findAllByBudgetWeekIn(List<BudgetWeek> budgetWeeks);

	List<EssentialWeekAllocation> findAllByEssentialExpenseMonthIn(List<EssentialExpenseMonth> essentialExpenseMonths);
}

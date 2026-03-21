package com.ssafy.amagetdone.domain.budget.model.repository;

import java.util.List;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetAdjustmentType;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetAdjustmentRepository extends JpaRepository<BudgetAdjustment, Integer> {

	List<BudgetAdjustment> findByBudgetWeekOrderByCreatedAtDesc(BudgetWeek budgetWeek);

	List<BudgetAdjustment> findByBudgetWeekOrderByCreatedAtAsc(BudgetWeek budgetWeek);

	List<BudgetAdjustment> findAllByBudgetWeekIn(List<BudgetWeek> budgetWeeks);

	void deleteAllByBudgetWeekInAndAdjustmentType(List<BudgetWeek> budgetWeeks, BudgetAdjustmentType adjustmentType);
}

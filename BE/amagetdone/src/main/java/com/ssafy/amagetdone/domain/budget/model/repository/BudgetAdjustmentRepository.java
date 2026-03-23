package com.ssafy.amagetdone.domain.budget.model.repository;

import java.util.List;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetAdjustmentType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetAdjustmentRepository extends JpaRepository<BudgetAdjustment, Integer> {

	@Modifying
	@Query("DELETE FROM BudgetAdjustment a WHERE a.budgetWeek.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Integer userId);

	List<BudgetAdjustment> findByBudgetWeekOrderByCreatedAtDesc(BudgetWeek budgetWeek);

	List<BudgetAdjustment> findByBudgetWeekOrderByCreatedAtAsc(BudgetWeek budgetWeek);

	List<BudgetAdjustment> findAllByBudgetWeekIn(List<BudgetWeek> budgetWeeks);

	void deleteAllByBudgetWeekInAndAdjustmentType(List<BudgetWeek> budgetWeeks, BudgetAdjustmentType adjustmentType);
}

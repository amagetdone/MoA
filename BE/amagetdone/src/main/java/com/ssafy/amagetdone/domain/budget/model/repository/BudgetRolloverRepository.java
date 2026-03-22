package com.ssafy.amagetdone.domain.budget.model.repository;

import java.util.Optional;
import java.util.List;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetRollover;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetRolloverRepository extends JpaRepository<BudgetRollover, Integer> {

	@Modifying
	@Query("DELETE FROM BudgetRollover r WHERE r.sourceBudgetWeek.user.id = :userId OR r.targetBudgetWeek.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Integer userId);

	Optional<BudgetRollover> findFirstByTargetBudgetWeekOrderByCreatedAtDesc(BudgetWeek targetBudgetWeek);

	void deleteAllByTargetBudgetWeekIn(List<BudgetWeek> targetBudgetWeeks);

	void deleteAllBySourceBudgetWeekIn(List<BudgetWeek> sourceBudgetWeeks);
}

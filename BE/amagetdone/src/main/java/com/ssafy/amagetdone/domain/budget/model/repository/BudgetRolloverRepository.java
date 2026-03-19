package com.ssafy.amagetdone.domain.budget.model.repository;

import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetRollover;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRolloverRepository extends JpaRepository<BudgetRollover, Integer> {

	Optional<BudgetRollover> findFirstByTargetBudgetWeekOrderByCreatedAtDesc(BudgetWeek targetBudgetWeek);
}

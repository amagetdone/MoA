package com.ssafy.amagetdone.domain.budget.model.repository;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetAdjustmentRepository extends JpaRepository<BudgetAdjustment, Integer> {
}

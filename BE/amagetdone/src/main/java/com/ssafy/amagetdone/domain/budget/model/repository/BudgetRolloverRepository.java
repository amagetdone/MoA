package com.ssafy.amagetdone.domain.budget.model.repository;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetRollover;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRolloverRepository extends JpaRepository<BudgetRollover, Integer> {
}

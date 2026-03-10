package com.ssafy.amagetdone.domain.budget.model.repository;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetWeekRepository extends JpaRepository<BudgetWeek, Integer> {
}

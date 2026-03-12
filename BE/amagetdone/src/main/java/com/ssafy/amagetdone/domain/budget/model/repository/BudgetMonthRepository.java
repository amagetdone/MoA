package com.ssafy.amagetdone.domain.budget.model.repository;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetMonth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetMonthRepository extends JpaRepository<BudgetMonth, Integer> {
}

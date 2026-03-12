package com.ssafy.amagetdone.domain.budget.model.repository;

import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseMonth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EssentialExpenseMonthRepository extends JpaRepository<EssentialExpenseMonth, Integer> {
}

package com.ssafy.amagetdone.domain.budget.model.repository;

import java.time.LocalDate;
import java.util.List;

import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseMonth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EssentialExpenseMonthRepository extends JpaRepository<EssentialExpenseMonth, Integer> {

	List<EssentialExpenseMonth> findAllByBudgetMonth_User_IdAndBudgetMonth_BaseDate(Integer userId, LocalDate baseDate);

	List<EssentialExpenseMonth> findAllByBudgetMonth_User_IdOrderByBudgetMonth_BaseDateAscIdAsc(Integer userId);
}

package com.ssafy.amagetdone.domain.budget.model.repository;

import java.time.LocalDate;
import java.util.List;

import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseMonth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EssentialExpenseMonthRepository extends JpaRepository<EssentialExpenseMonth, Integer> {

	@Modifying
	@Query("DELETE FROM EssentialExpenseMonth e WHERE e.budgetMonth.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Integer userId);

	List<EssentialExpenseMonth> findAllByBudgetMonth_User_IdAndBudgetMonth_BaseDate(Integer userId, LocalDate baseDate);

	List<EssentialExpenseMonth> findAllByBudgetMonth_User_IdOrderByBudgetMonth_BaseDateAscIdAsc(Integer userId);
}

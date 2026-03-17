package com.ssafy.amagetdone.domain.budget.model.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetWeekRepository extends JpaRepository<BudgetWeek, Integer> {

	List<BudgetWeek> findAllByUserIdAndWeekStartDateGreaterThanEqualAndWeekEndDateLessThanEqualOrderByWeekStartDateAsc(
		Integer userId,
		LocalDate weekStartDate,
		LocalDate weekEndDate
	);

	void deleteAllByUserIdAndWeekEndDateGreaterThanEqualAndWeekStartDateLessThanEqual(
		Integer userId,
		LocalDate weekStartDate,
		LocalDate weekEndDate
	);

	Optional<BudgetWeek> findTopByOrderByIdDesc();

	Optional<BudgetWeek> findTopByUserIdOrderByWeekSequenceDesc(Integer userId);
}

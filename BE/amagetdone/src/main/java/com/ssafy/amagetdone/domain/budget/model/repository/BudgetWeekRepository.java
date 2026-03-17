package com.ssafy.amagetdone.domain.budget.model.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.member.model.entity.User;

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

	Optional<BudgetWeek> findByUserAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(User user, LocalDate date, LocalDate date2);

	List<BudgetWeek> findByUserAndWeekStartDateBetweenOrderByWeekSequence(User user, LocalDate start, LocalDate end);

	Optional<BudgetWeek> findFirstByUserOrderByWeekEndDateDesc(User user);

	Optional<BudgetWeek> findFirstByUserAndWeekSequenceGreaterThanOrderByWeekSequenceAsc(User user, Integer weekSequence);
}

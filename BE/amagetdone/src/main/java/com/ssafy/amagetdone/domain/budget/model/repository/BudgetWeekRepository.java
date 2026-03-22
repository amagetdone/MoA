package com.ssafy.amagetdone.domain.budget.model.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetWeekRepository extends JpaRepository<BudgetWeek, Integer> {

	@Modifying
	@Query("DELETE FROM BudgetWeek w WHERE w.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Integer userId);

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

	Optional<BudgetWeek> findFirstByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualOrderByWeekSequenceAsc(
		Integer userId,
		LocalDate weekStartDate,
		LocalDate weekEndDate
	);

	Optional<BudgetWeek> findByUserAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(User user, LocalDate date, LocalDate date2);

	List<BudgetWeek> findByUserAndWeekStartDateBetweenOrderByWeekSequence(User user, LocalDate start, LocalDate end);

	Optional<BudgetWeek> findFirstByUserOrderByWeekEndDateDesc(User user);

	Optional<BudgetWeek> findFirstByUserAndWeekSequenceGreaterThanOrderByWeekSequenceAsc(User user, Integer weekSequence);

	List<BudgetWeek> findAllByUser_IdOrderByWeekStartDateAsc(Integer userId);

	List<BudgetWeek> findAllByUser_IdOrderByWeekSequenceAsc(Integer userId);
}

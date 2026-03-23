package com.ssafy.amagetdone.domain.budget.model.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeek;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EssentialExpenseSnapshotWeekRepository extends JpaRepository<EssentialExpenseSnapshotWeek, Integer> {

	@Modifying
	@Query("DELETE FROM EssentialExpenseSnapshotWeek s WHERE s.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Integer userId);

	Optional<EssentialExpenseSnapshotWeek> findByUser_IdAndYearWeek(Integer userId, String yearWeek);

	Optional<EssentialExpenseSnapshotWeek> findByUser_IdAndYearWeekAndAnalyzedAt(
		Integer userId,
		String yearWeek,
		LocalDateTime analyzedAt
	);

	List<EssentialExpenseSnapshotWeek> findAllByUser_IdAndWeekStartDateBetweenOrderByWeekStartDate(
		Integer userId,
		LocalDate startDate,
		LocalDate endDate
	);

	List<EssentialExpenseSnapshotWeek> findAllByUser_IdAndAnalyzedAtAndWeekStartDateBetweenOrderByWeekStartDate(
		Integer userId,
		LocalDateTime analyzedAt,
		LocalDate startDate,
		LocalDate endDate
	);
}

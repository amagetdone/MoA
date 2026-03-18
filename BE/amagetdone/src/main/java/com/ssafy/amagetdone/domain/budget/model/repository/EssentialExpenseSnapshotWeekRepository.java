package com.ssafy.amagetdone.domain.budget.model.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeek;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EssentialExpenseSnapshotWeekRepository extends JpaRepository<EssentialExpenseSnapshotWeek, Integer> {

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

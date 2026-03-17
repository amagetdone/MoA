package com.ssafy.amagetdone.domain.budget.model.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeek;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EssentialExpenseSnapshotWeekRepository extends JpaRepository<EssentialExpenseSnapshotWeek, Integer> {

	Optional<EssentialExpenseSnapshotWeek> findByUser_IdAndYearWeek(Integer userId, String yearWeek);

	List<EssentialExpenseSnapshotWeek> findAllByUser_IdAndWeekStartDateBetweenOrderByWeekStartDate(
		Integer userId,
		LocalDate startDate,
		LocalDate endDate
	);
}

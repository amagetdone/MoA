package com.ssafy.amagetdone.domain.budget.model.repository;

import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeekCategory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EssentialExpenseSnapshotWeekCategoryRepository
	extends JpaRepository<EssentialExpenseSnapshotWeekCategory, Integer> {
}

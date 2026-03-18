package com.ssafy.amagetdone.domain.budget.model.repository;

import java.util.Collection;
import java.util.List;

import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeekCategory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EssentialExpenseSnapshotWeekCategoryRepository
	extends JpaRepository<EssentialExpenseSnapshotWeekCategory, Integer> {

	List<EssentialExpenseSnapshotWeekCategory> findAllBySnapshotWeek_IdIn(Collection<Integer> snapshotWeekIds);
}

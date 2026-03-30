package com.ssafy.amagetdone.domain.budget.model.repository;

import java.util.Collection;
import java.util.List;

import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeekCategory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EssentialExpenseSnapshotWeekCategoryRepository
	extends JpaRepository<EssentialExpenseSnapshotWeekCategory, Integer> {

	@Modifying
	@Query("DELETE FROM EssentialExpenseSnapshotWeekCategory c WHERE c.snapshotWeek.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Integer userId);

	List<EssentialExpenseSnapshotWeekCategory> findAllBySnapshotWeek_IdIn(Collection<Integer> snapshotWeekIds);
}

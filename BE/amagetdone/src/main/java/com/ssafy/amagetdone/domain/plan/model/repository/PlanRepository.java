package com.ssafy.amagetdone.domain.plan.model.repository;

import java.util.List;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanAiPipelineStatus;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanRepository extends JpaRepository<Plan, Integer> {

	Optional<Plan> findByIdAndUserId(Integer id, Integer userId);

	List<Plan> findAllByUser_IdAndStatus(Integer userId, PlanStatus status);

	long countByUser_IdAndStatus(Integer userId, PlanStatus status);

	@Query("select coalesce(sum(p.savedAmount), 0) from Plan p where p.user.id = :userId")
	Long sumSavedAmountByUserId(@Param("userId") Integer userId);

	List<Plan> findAllByUserIdAndPriorityGreaterThanEqualOrderByPriorityAsc(Integer userId, Integer priority);

	List<Plan> findAllByStatusAndStartDateLessThanEqual(PlanStatus status, LocalDate date);

	List<Plan> findAllByStatusAndStartDateLessThanEqualAndAiPipelineStatus(
		PlanStatus status,
		LocalDate date,
		PlanAiPipelineStatus aiPipelineStatus
	);

	boolean existsByUserId(Integer userId);

	void deleteByUserId(Integer userId);
}

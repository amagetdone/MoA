package com.ssafy.amagetdone.domain.plan.model.repository;

import java.time.LocalDate;
import java.util.Collection;
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

	Optional<Plan> findByIdAndUserIdAndDeletedFalse(Integer id, Integer userId);

	List<Plan> findAllByUser_IdAndStatus(Integer userId, PlanStatus status);

	List<Plan> findAllByUser_IdAndStatusAndDeletedFalse(Integer userId, PlanStatus status);

	List<Plan> findAllByStatus(PlanStatus status);

	List<Plan> findAllByStatusAndDeletedFalse(PlanStatus status);

	long countByUser_IdAndStatus(Integer userId, PlanStatus status);

	long countByUser_IdAndStatusAndDeletedFalse(Integer userId, PlanStatus status);

	@Query("select coalesce(sum(p.savedAmount), 0) from Plan p where p.user.id = :userId")
	Long sumSavedAmountByUserId(@Param("userId") Integer userId);

	@Query("select coalesce(sum(p.savedAmount), 0) from Plan p where p.user.id = :userId and p.deleted = false")
	Long sumSavedAmountByUserIdAndDeletedFalse(@Param("userId") Integer userId);

	List<Plan> findAllByUserId(Integer userId);

	List<Plan> findAllByUserIdAndDeletedFalse(Integer userId);

	List<Plan> findAllByUserIdAndPriorityGreaterThanEqualOrderByPriorityAsc(Integer userId, Integer priority);

	List<Plan> findAllByUserIdAndDeletedFalseAndStatusInAndPriorityGreaterThanEqualOrderByPriorityAsc(
		Integer userId,
		Collection<PlanStatus> statuses,
		Integer priority
	);

	List<Plan> findAllByUserIdAndDeletedFalseAndStatusInAndPriorityGreaterThanOrderByPriorityAsc(
		Integer userId,
		Collection<PlanStatus> statuses,
		Integer priority
	);

	List<Plan> findAllByStatusAndStartDateLessThanEqual(PlanStatus status, LocalDate date);

	List<Plan> findAllByStatusAndStartDateLessThanEqualAndDeletedFalse(PlanStatus status, LocalDate date);

	List<Plan> findAllByStatusAndStartDateLessThanEqualAndAiPipelineStatus(
		PlanStatus status,
		LocalDate date,
		PlanAiPipelineStatus aiPipelineStatus
	);

	List<Plan> findAllByStatusAndStartDateLessThanEqualAndAiPipelineStatusAndDeletedFalse(
		PlanStatus status,
		LocalDate date,
		PlanAiPipelineStatus aiPipelineStatus
	);

	boolean existsByUserId(Integer userId);

	boolean existsByUserIdAndDeletedFalse(Integer userId);

	void deleteByUserId(Integer userId);
}

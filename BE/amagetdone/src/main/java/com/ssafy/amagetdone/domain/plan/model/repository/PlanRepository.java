package com.ssafy.amagetdone.domain.plan.model.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanAiPipelineStatus;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;
import com.ssafy.amagetdone.domain.plan.model.vo.PyanTaskStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanRepository extends JpaRepository<Plan, Integer> {

	Optional<Plan> findByIdAndUserId(Integer id, Integer userId);

	Optional<Plan> findByIdAndUserIdAndDeletedFalse(Integer id, Integer userId);

	List<Plan> findAllByUser_IdAndStatus(Integer userId, PlanStatus status);

	List<Plan> findAllByUser_IdAndStatusAndDeletedFalse(Integer userId, PlanStatus status);

	List<Plan> findAllByUserIdAndDeletedFalseAndStatusIn(Integer userId, Collection<PlanStatus> statuses);

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

	List<Plan> findAllByAiPipelineStatusAndDeletedFalseAndStatusNotAndPyanStatusUrlIsNotNull(
		PlanAiPipelineStatus aiPipelineStatus,
		PlanStatus status
	);

	boolean existsByUserId(Integer userId);

	boolean existsByUserIdAndDeletedFalse(Integer userId);

	void deleteByUserId(Integer userId);

	@Modifying(flushAutomatically = true)
	@Query("""
		update Plan p
		set p.aiPipelineStatus = :processingStatus,
			p.pyanRequestKey = coalesce(p.pyanRequestKey, :pyanRequestKey),
			p.version = p.version + 1
		where p.id = :planId
			and p.user.id = :userId
			and p.deleted = false
			and p.aiPipelineStatus = :pendingStatus
			and p.status <> :cancelledStatus
		""")
	int markAiProcessing(
		@Param("planId") Integer planId,
		@Param("userId") Integer userId,
		@Param("processingStatus") PlanAiPipelineStatus processingStatus,
		@Param("pyanRequestKey") String pyanRequestKey,
		@Param("pendingStatus") PlanAiPipelineStatus pendingStatus,
		@Param("cancelledStatus") PlanStatus cancelledStatus
	);

	@Modifying(flushAutomatically = true)
	@Query("""
		update Plan p
		set p.planUrl = :planUrl,
			p.version = p.version + 1
		where p.id = :planId
			and p.user.id = :userId
			and p.deleted = false
			and p.status <> :cancelledStatus
		""")
	int updateAiImageKey(
		@Param("planId") Integer planId,
		@Param("userId") Integer userId,
		@Param("planUrl") String planUrl,
		@Param("cancelledStatus") PlanStatus cancelledStatus
	);

	@Modifying(flushAutomatically = true)
	@Query("""
		update Plan p
		set p.pyanStatus = :pyanStatus,
			p.pyanStatusUrl = :statusUrl,
			p.pyanLastError = null,
			p.version = p.version + 1
		where p.id = :planId
			and p.user.id = :userId
			and p.deleted = false
			and p.status <> :cancelledStatus
		""")
	int registerPyanTask(
		@Param("planId") Integer planId,
		@Param("userId") Integer userId,
		@Param("pyanStatus") PyanTaskStatus pyanStatus,
		@Param("statusUrl") String statusUrl,
		@Param("cancelledStatus") PlanStatus cancelledStatus
	);

	@Modifying(flushAutomatically = true)
	@Query("""
		update Plan p
		set p.videoUrl = :videoUrl,
			p.aiPipelineStatus = :successStatus,
			p.pyanStatus = :pyanStatus,
			p.pyanLastError = null,
			p.version = p.version + 1
		where p.id = :planId
			and p.user.id = :userId
			and p.deleted = false
			and p.status <> :cancelledStatus
		""")
	int completeAiSuccess(
		@Param("planId") Integer planId,
		@Param("userId") Integer userId,
		@Param("videoUrl") String videoUrl,
		@Param("pyanStatus") PyanTaskStatus pyanStatus,
		@Param("successStatus") PlanAiPipelineStatus successStatus,
		@Param("cancelledStatus") PlanStatus cancelledStatus
	);

	@Modifying(flushAutomatically = true)
	@Query("""
		update Plan p
		set p.pyanStatus = :pyanStatus,
			p.pyanLastError = :pyanLastError,
			p.version = p.version + 1
		where p.id = :planId
			and p.user.id = :userId
			and p.deleted = false
			and p.status <> :cancelledStatus
		""")
	int syncPyanProgress(
		@Param("planId") Integer planId,
		@Param("userId") Integer userId,
		@Param("pyanStatus") PyanTaskStatus pyanStatus,
		@Param("pyanLastError") String pyanLastError,
		@Param("cancelledStatus") PlanStatus cancelledStatus
	);

	@Modifying(flushAutomatically = true)
	@Query("""
		update Plan p
		set p.aiPipelineStatus = :failedStatus,
			p.pyanStatus = :pyanStatus,
			p.pyanLastError = :pyanLastError,
			p.aiRetryCount = p.aiRetryCount + 1,
			p.version = p.version + 1
		where p.id = :planId
			and p.user.id = :userId
			and p.deleted = false
			and p.status <> :cancelledStatus
		""")
	int markAiFailed(
		@Param("planId") Integer planId,
		@Param("userId") Integer userId,
		@Param("failedStatus") PlanAiPipelineStatus failedStatus,
		@Param("pyanStatus") PyanTaskStatus pyanStatus,
		@Param("pyanLastError") String pyanLastError,
		@Param("cancelledStatus") PlanStatus cancelledStatus
	);

	@Modifying(flushAutomatically = true)
	@Query("""
		update Plan p
		set p.savedAmount = case
				when coalesce(p.savedAmount, 0) + :delta < 0 then 0
				else coalesce(p.savedAmount, 0) + :delta
			end,
			p.version = p.version + 1
		where p.id = :planId
		""")
	int adjustSavedAmount(
		@Param("planId") Integer planId,
		@Param("delta") Long delta
	);
}

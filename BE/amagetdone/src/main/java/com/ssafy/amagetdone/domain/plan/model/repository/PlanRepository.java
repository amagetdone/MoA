package com.ssafy.amagetdone.domain.plan.model.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Integer> {

	Optional<Plan> findByIdAndUserId(Integer id, Integer userId);

	List<Plan> findAllByUserIdAndPriorityGreaterThanEqualOrderByPriorityAsc(Integer userId, Integer priority);

	List<Plan> findAllByStatusAndStartDateLessThanEqual(PlanStatus status, LocalDate date);

	boolean existsByUserId(Integer userId);

	void deleteByUserId(Integer userId);
}

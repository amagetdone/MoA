package com.ssafy.amagetdone.domain.plan.model.repository;

import java.util.Optional;

import com.ssafy.amagetdone.domain.plan.model.entity.Plan;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Integer> {

	Optional<Plan> findByIdAndUserId(Integer id, Integer userId);

	boolean existsByUserId(Integer userId);

	void deleteByUserId(Integer userId);
}

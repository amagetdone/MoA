package com.ssafy.amagetdone.domain.plan.model.repository;

import com.ssafy.amagetdone.domain.plan.model.entity.Plan;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Integer> {
}

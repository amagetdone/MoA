package com.ssafy.amagetdone.domain.plan.model.repository;

import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanWeekAllocationRepository extends JpaRepository<PlanWeekAllocation, Integer> {
}

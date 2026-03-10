package com.ssafy.amagetdone.domain.budget.model.repository;

import com.ssafy.amagetdone.domain.budget.model.entity.EssentialWeekAllocation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EssentialWeekAllocationRepository extends JpaRepository<EssentialWeekAllocation, Integer> {
}

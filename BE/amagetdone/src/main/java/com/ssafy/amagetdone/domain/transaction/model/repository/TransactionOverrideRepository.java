package com.ssafy.amagetdone.domain.transaction.model.repository;

import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionOverride;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionOverrideRepository extends JpaRepository<TransactionOverride, Integer> {
}

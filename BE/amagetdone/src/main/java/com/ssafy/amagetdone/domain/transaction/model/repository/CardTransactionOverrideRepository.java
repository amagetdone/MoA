package com.ssafy.amagetdone.domain.transaction.model.repository;

import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CardTransactionOverrideRepository extends JpaRepository<CardTransactionOverride, Integer> {
}

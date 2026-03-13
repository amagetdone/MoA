package com.ssafy.amagetdone.domain.transaction.model.repository;

import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisResult;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionAnalysisResultRepository extends JpaRepository<TransactionAnalysisResult, Integer> {
}

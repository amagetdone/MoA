package com.ssafy.amagetdone.domain.transaction.model.repository;

import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisJob;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionAnalysisJobRepository extends JpaRepository<TransactionAnalysisJob, String> {
}

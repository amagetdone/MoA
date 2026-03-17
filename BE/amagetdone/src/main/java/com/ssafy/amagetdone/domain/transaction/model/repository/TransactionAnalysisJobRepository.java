package com.ssafy.amagetdone.domain.transaction.model.repository;

import java.util.Optional;

import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisJob;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionAnalysisJobStatus;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionAnalysisJobRepository extends JpaRepository<TransactionAnalysisJob, String> {

	Optional<TransactionAnalysisJob> findFirstByUser_IdAndStatusOrderByRequestedAtDesc(
		Integer userId,
		TransactionAnalysisJobStatus status
	);

	boolean existsByUser_IdAndStatus(Integer userId, TransactionAnalysisJobStatus status);
}

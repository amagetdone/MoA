package com.ssafy.amagetdone.domain.transaction.model.repository;

import java.util.Optional;

import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisMetadata;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionAnalysisMetadataRepository extends JpaRepository<TransactionAnalysisMetadata, Integer> {

	Optional<TransactionAnalysisMetadata> findTopByUser_IdOrderByAnalyzedAtDesc(Integer userId);

	Optional<TransactionAnalysisMetadata> findTopByUser_IdAndRequestedMonthsOrderByAnalyzedAtDesc(
		Integer userId,
		Integer requestedMonths
	);
}

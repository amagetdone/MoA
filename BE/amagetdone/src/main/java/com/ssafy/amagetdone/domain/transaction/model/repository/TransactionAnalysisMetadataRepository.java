package com.ssafy.amagetdone.domain.transaction.model.repository;

import java.util.Optional;

import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisMetadata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionAnalysisMetadataRepository extends JpaRepository<TransactionAnalysisMetadata, Integer> {

	@Modifying
	@Query("DELETE FROM TransactionAnalysisMetadata m WHERE m.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Integer userId);

	Optional<TransactionAnalysisMetadata> findTopByUser_IdOrderByAnalyzedAtDesc(Integer userId);

	Optional<TransactionAnalysisMetadata> findTopByUser_IdAndRequestedMonthsOrderByAnalyzedAtDesc(
		Integer userId,
		Integer requestedMonths
	);
}

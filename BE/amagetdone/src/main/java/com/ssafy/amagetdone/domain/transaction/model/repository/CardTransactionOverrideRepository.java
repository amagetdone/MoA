package com.ssafy.amagetdone.domain.transaction.model.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CardTransactionOverrideRepository extends JpaRepository<CardTransactionOverride, Integer> {

	List<CardTransactionOverride> findAllByCard_CardUniqueNo(Long cardUniqueNo);

	void deleteByCard_CardUniqueNo(Long cardUniqueNo);

	Optional<CardTransactionOverride> findTopByCard_CardUniqueNoOrderByTransactionDateTimeDesc(Long cardUniqueNo);

	boolean existsByCard_CardUniqueNoAndTransactionUniqueNo(Long cardUniqueNo, Long transactionUniqueNo);

	List<CardTransactionOverride>
		findAllByCard_WithdrawalAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(
			Integer userId,
			LocalDateTime fromDateTime,
			LocalDateTime toDateTime
		);
}

package com.ssafy.amagetdone.domain.transaction.model.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardTransactionOverrideRepository extends JpaRepository<CardTransactionOverride, Integer> {

	@Modifying
	@Query("DELETE FROM CardTransactionOverride o WHERE o.card.withdrawalAccount.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Integer userId);

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

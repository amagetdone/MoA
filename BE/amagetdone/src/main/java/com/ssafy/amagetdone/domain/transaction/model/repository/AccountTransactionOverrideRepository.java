package com.ssafy.amagetdone.domain.transaction.model.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountTransactionOverrideRepository extends JpaRepository<AccountTransactionOverride, Integer> {

	Optional<AccountTransactionOverride> findTopByAccount_IdOrderByTransactionDateTimeDesc(String accountId);

	boolean existsByAccount_IdAndTransactionUniqueNo(String accountId, Long transactionUniqueNo);

	List<AccountTransactionOverride> findAllByAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(
		Integer userId,
		LocalDateTime fromDateTime,
		LocalDateTime toDateTime
	);
}

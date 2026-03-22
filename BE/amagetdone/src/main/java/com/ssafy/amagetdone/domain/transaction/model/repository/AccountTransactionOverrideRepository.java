package com.ssafy.amagetdone.domain.transaction.model.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountTransactionOverrideRepository extends JpaRepository<AccountTransactionOverride, Integer> {

	@Modifying
	@Query("DELETE FROM AccountTransactionOverride o WHERE o.account.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Integer userId);

	List<AccountTransactionOverride> findAllByAccount_Id(String accountId);

	void deleteByAccount_Id(String accountId);

	Optional<AccountTransactionOverride> findTopByAccount_IdOrderByTransactionDateTimeDesc(String accountId);

	boolean existsByAccount_IdAndTransactionUniqueNo(String accountId, Long transactionUniqueNo);

	List<AccountTransactionOverride> findAllByAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(
		Integer userId,
		LocalDateTime fromDateTime,
		LocalDateTime toDateTime
	);
}

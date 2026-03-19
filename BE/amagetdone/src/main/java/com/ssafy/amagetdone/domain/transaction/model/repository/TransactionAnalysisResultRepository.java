package com.ssafy.amagetdone.domain.transaction.model.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisResult;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionAnalysisResultRepository extends JpaRepository<TransactionAnalysisResult, Integer> {

	void deleteByTransaction_IdIn(Collection<Integer> transactionIds);

	Optional<TransactionAnalysisResult> findByTransaction_Id(Integer transactionId);

	List<TransactionAnalysisResult> findAllByTransaction_IdIn(Collection<Integer> transactionIds);

	List<TransactionAnalysisResult> findAllByTransaction_IdInAndAnalyzedAt(
		Collection<Integer> transactionIds,
		LocalDateTime analyzedAt
	);

	@Query("""
		select r
		from TransactionAnalysisResult r
		join r.transaction t
		left join treat(t as AccountTransactionOverride) accountTransaction
		left join accountTransaction.account account
		left join treat(t as CardTransactionOverride) cardTransaction
		left join cardTransaction.card card
		left join card.withdrawalAccount withdrawalAccount
		where (account.user.id = :userId or withdrawalAccount.user.id = :userId)
		  and r.analyzedAt = :analyzedAt
		  and t.transactionDateTime between :fromDateTime and :toDateTime
		order by t.transactionDateTime asc, r.id asc
		""")
	List<TransactionAnalysisResult> findAllByUserIdAndAnalyzedAtAndTransactionDateTimeBetween(
		@Param("userId") Integer userId,
		@Param("analyzedAt") LocalDateTime analyzedAt,
		@Param("fromDateTime") LocalDateTime fromDateTime,
		@Param("toDateTime") LocalDateTime toDateTime
	);

	@Query("""
		select r
		from TransactionAnalysisResult r
		join r.transaction t
		left join treat(t as AccountTransactionOverride) accountTransaction
		left join accountTransaction.account account
		left join treat(t as CardTransactionOverride) cardTransaction
		left join cardTransaction.card card
		left join card.withdrawalAccount withdrawalAccount
		where (account.user.id = :userId or withdrawalAccount.user.id = :userId)
		  and r.analyzedAt = :analyzedAt
		  and t.transactionDateTime between :fromDateTime and :toDateTime
		order by t.transactionDateTime asc, r.id asc
		""")
	List<TransactionAnalysisResult> findAllByUserIdAndAnalyzedAtAndTransactionDateTimeBetweenOrderByTransactionDateTimeAsc(
		@Param("userId") Integer userId,
		@Param("analyzedAt") LocalDateTime analyzedAt,
		@Param("fromDateTime") LocalDateTime fromDateTime,
		@Param("toDateTime") LocalDateTime toDateTime
	);

	@Query("""
		select r.analysisIncluded as analysisIncluded,
		       r.excludedReason as excludedReason,
		       r.expenseScope as expenseScope,
		       coalesce(sum(t.transactionBalance), 0) as totalAmount,
		       count(r.id) as transactionCount
		from TransactionAnalysisResult r
		join r.transaction t
		left join treat(t as AccountTransactionOverride) accountTransaction
		left join accountTransaction.account account
		left join treat(t as CardTransactionOverride) cardTransaction
		left join cardTransaction.card card
		left join card.withdrawalAccount withdrawalAccount
		where (account.user.id = :userId or withdrawalAccount.user.id = :userId)
		  and t.transactionDateTime between :fromDateTime and :toDateTime
		group by r.analysisIncluded, r.excludedReason, r.expenseScope
		""")
	List<TransactionAnalysisAmountView> summarizeByUserIdAndPeriod(
		@Param("userId") Integer userId,
		@Param("fromDateTime") LocalDateTime fromDateTime,
		@Param("toDateTime") LocalDateTime toDateTime
	);
}

package com.ssafy.amagetdone.domain.account.model.repository;

import java.util.List;

import com.ssafy.amagetdone.domain.account.model.entity.Card;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, Long> {

	@Modifying
	@Query("DELETE FROM Card c WHERE c.withdrawalAccount.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Integer userId);

	java.util.Optional<Card> findByCardNo(String cardNo);

	List<Card> findAllByWithdrawalAccount_User_Id(Integer userId);

	List<Card> findAllByWithdrawalAccount_User_IdAndWithdrawalAccount_VerifiedTrueAndWithdrawalAccount_ActiveTrue(
		Integer userId
	);
}

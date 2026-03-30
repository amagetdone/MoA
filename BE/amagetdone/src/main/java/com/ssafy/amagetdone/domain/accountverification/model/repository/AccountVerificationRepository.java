package com.ssafy.amagetdone.domain.accountverification.model.repository;

import java.util.Optional;

import com.ssafy.amagetdone.domain.accountverification.model.entity.AccountVerification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountVerificationRepository extends JpaRepository<AccountVerification, Long> {

	@Modifying
	@Query("DELETE FROM AccountVerification v WHERE v.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Integer userId);

	Optional<AccountVerification> findTopByUser_IdAndAccountNoOrderByCreatedAtDesc(Integer userId, String accountNo);
}

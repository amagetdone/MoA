package com.ssafy.amagetdone.domain.accountverification.model.repository;

import java.util.Optional;

import com.ssafy.amagetdone.domain.accountverification.model.entity.AccountVerification;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountVerificationRepository extends JpaRepository<AccountVerification, Long> {

	Optional<AccountVerification> findTopByAccount_User_IdAndAccount_AccountNoOrderByCreatedAtDesc(Integer userId, String accountNo);
}

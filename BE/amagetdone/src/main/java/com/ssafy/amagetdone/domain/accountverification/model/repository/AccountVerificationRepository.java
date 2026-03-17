package com.ssafy.amagetdone.domain.accountverification.model.repository;

import java.util.Optional;

import com.ssafy.amagetdone.domain.accountverification.model.entity.AccountVerification;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountVerificationRepository extends JpaRepository<AccountVerification, Long> {

	Optional<AccountVerification> findTopByUser_IdAndAccountNoOrderByCreatedAtDesc(Integer userId, String accountNo);
}

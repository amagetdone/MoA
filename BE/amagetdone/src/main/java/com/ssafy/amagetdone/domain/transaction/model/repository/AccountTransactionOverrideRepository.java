package com.ssafy.amagetdone.domain.transaction.model.repository;

import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountTransactionOverrideRepository extends JpaRepository<AccountTransactionOverride, Integer> {
}

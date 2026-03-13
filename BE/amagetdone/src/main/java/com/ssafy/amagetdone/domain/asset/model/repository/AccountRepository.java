package com.ssafy.amagetdone.domain.asset.model.repository;

import com.ssafy.amagetdone.domain.asset.model.entity.Account;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {

	boolean existsByUserId(Integer userId);

	void deleteByUserId(Integer userId);
}

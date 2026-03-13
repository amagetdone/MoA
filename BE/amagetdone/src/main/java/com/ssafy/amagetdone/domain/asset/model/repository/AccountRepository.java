package com.ssafy.amagetdone.domain.asset.model.repository;

import java.util.List;

import com.ssafy.amagetdone.domain.asset.model.entity.Account;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {

	List<Account> findAllByUser_Id(Integer userId);

	boolean existsByUserId(Integer userId);

	void deleteByUserId(Integer userId);
}

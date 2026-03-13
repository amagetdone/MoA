package com.ssafy.amagetdone.domain.account.model.repository;

import com.ssafy.amagetdone.domain.account.model.entity.Merchant;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, Integer> {
}

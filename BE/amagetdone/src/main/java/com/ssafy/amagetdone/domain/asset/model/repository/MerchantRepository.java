package com.ssafy.amagetdone.domain.asset.model.repository;

import com.ssafy.amagetdone.domain.asset.model.entity.Merchant;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, Integer> {
}

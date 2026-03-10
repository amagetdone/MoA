package com.ssafy.amagetdone.domain.asset.model.repository;

import com.ssafy.amagetdone.domain.asset.model.entity.Pocket;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PocketRepository extends JpaRepository<Pocket, String> {
}

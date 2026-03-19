package com.ssafy.amagetdone.domain.account.model.repository;

import com.ssafy.amagetdone.domain.account.model.entity.Pocket;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PocketRepository extends JpaRepository<Pocket, String> {
}

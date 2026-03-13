package com.ssafy.amagetdone.domain.account.model.repository;

import com.ssafy.amagetdone.domain.account.model.entity.Card;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {
}

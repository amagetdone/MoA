package com.ssafy.amagetdone.domain.asset.model.repository;

import java.util.List;

import com.ssafy.amagetdone.domain.asset.model.entity.Card;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {

	List<Card> findAllByWithdrawalAccount_User_Id(Integer userId);
}

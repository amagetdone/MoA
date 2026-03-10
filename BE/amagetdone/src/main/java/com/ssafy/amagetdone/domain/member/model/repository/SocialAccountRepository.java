package com.ssafy.amagetdone.domain.member.model.repository;

import com.ssafy.amagetdone.domain.member.model.entity.SocialAccount;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Integer> {
}

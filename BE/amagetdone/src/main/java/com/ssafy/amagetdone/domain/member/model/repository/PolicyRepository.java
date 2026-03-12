package com.ssafy.amagetdone.domain.member.model.repository;

import com.ssafy.amagetdone.domain.member.model.entity.Policy;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, Integer> {
}

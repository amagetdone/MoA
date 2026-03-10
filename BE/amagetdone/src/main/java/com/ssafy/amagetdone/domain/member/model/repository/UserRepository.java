package com.ssafy.amagetdone.domain.member.model.repository;

import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
}

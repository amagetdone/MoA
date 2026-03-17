package com.ssafy.amagetdone.domain.member.model.repository;

import java.util.Optional;

import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {

	Optional<User> findByUserKey(String userKey);
}

package com.ssafy.amagetdone.domain.member.model.repository;

import java.util.Optional;

import com.ssafy.amagetdone.domain.member.model.entity.User;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Integer> {

	Optional<User> findByUserKey(String userKey);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.id = :userId")
	Optional<User> findByIdForUpdate(@Param("userId") Integer userId);
}

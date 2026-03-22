package com.ssafy.amagetdone.domain.account.model.repository;

import com.ssafy.amagetdone.domain.account.model.entity.Pocket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PocketRepository extends JpaRepository<Pocket, String> {

	@Modifying
	@Query("DELETE FROM Pocket p WHERE p.account.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Integer userId);
}

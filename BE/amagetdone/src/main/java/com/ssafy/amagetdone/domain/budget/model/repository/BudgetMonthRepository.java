package com.ssafy.amagetdone.domain.budget.model.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetMonth;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetMonthRepository extends JpaRepository<BudgetMonth, Integer> {

	@Modifying
	@Query("DELETE FROM BudgetMonth m WHERE m.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Integer userId);

	Optional<BudgetMonth> findByUserAndBaseDate(User user, LocalDate baseDate);

	Optional<BudgetMonth> findByUser_IdAndBaseDate(Integer userId, LocalDate baseDate);

	List<BudgetMonth> findAllByUser_IdOrderByBaseDateAsc(Integer userId);
}

package com.ssafy.amagetdone.domain.budget.model.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetMonth;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetMonthRepository extends JpaRepository<BudgetMonth, Integer> {

	Optional<BudgetMonth> findByUserAndBaseDate(User user, LocalDate baseDate);

	Optional<BudgetMonth> findByUser_IdAndBaseDate(Integer userId, LocalDate baseDate);

	List<BudgetMonth> findAllByUser_IdOrderByBaseDateAsc(Integer userId);
}

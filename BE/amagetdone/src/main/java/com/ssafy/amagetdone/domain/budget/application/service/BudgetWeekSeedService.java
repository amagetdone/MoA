package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetWeekSeedRequest;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetWeekSeedResponse;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.domain.plan.exception.PlanErrorCode;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetWeekSeedService {

	private final BudgetWeekRepository budgetWeekRepository;
	private final UserRepository userRepository;

	public BudgetWeekSeedService(BudgetWeekRepository budgetWeekRepository, UserRepository userRepository) {
		this.budgetWeekRepository = budgetWeekRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public BudgetWeekSeedResponse seed(Integer userId, BudgetWeekSeedRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new PlanException(PlanErrorCode.PLAN_OWNER_NOT_FOUND));

		LocalDate firstWeekStart = request.startDate().with(DayOfWeek.MONDAY);
		LocalDate lastWeekEnd = request.endDate().with(DayOfWeek.SUNDAY);

		budgetWeekRepository.deleteAllByUserIdAndWeekEndDateGreaterThanEqualAndWeekStartDateLessThanEqual(
			userId,
			firstWeekStart,
			lastWeekEnd
		);

		int nextId = budgetWeekRepository.findTopByOrderByIdDesc()
			.map(BudgetWeek::getId)
			.map(id -> id + 1)
			.orElse(1);
		int nextWeekSequence = budgetWeekRepository.findTopByUserIdOrderByWeekSequenceDesc(userId)
			.map(BudgetWeek::getWeekSequence)
			.map(sequence -> sequence + 1)
			.orElse(1);

		List<BudgetWeek> budgetWeeks = new ArrayList<>();
		LocalDate weekStart = firstWeekStart;
		while (!weekStart.isAfter(request.endDate())) {
			LocalDate weekEnd = weekStart.plusDays(6);
			budgetWeeks.add(BudgetWeek.create(nextId++, user, nextWeekSequence++, weekStart, weekEnd));
			weekStart = weekStart.plusWeeks(1);
		}

		budgetWeekRepository.saveAll(budgetWeeks);

		BudgetWeek firstWeek = budgetWeeks.get(0);
		BudgetWeek lastWeek = budgetWeeks.get(budgetWeeks.size() - 1);

		return new BudgetWeekSeedResponse(
			budgetWeeks.size(),
			firstWeek.getWeekStartDate(),
			lastWeek.getWeekEndDate()
		);
	}
}

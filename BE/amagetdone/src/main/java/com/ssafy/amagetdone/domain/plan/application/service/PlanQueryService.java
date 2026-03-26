package com.ssafy.amagetdone.domain.plan.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanDetailResponse;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanWeeklyPlansPageResponse;
import com.ssafy.amagetdone.domain.plan.exception.PlanErrorCode;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlanQueryService {

	private final PlanRepository planRepository;
	private final PlanWeekAllocationRepository planWeekAllocationRepository;
	private final BudgetWeekRepository budgetWeekRepository;
	private final Clock clock;

	public PlanQueryService(
		PlanRepository planRepository,
		PlanWeekAllocationRepository planWeekAllocationRepository,
		BudgetWeekRepository budgetWeekRepository,
		Clock clock
	) {
		this.planRepository = planRepository;
		this.planWeekAllocationRepository = planWeekAllocationRepository;
		this.budgetWeekRepository = budgetWeekRepository;
		this.clock = clock;
	}

	public PlanDetailResponse getPlanDetail(Integer userId, Integer planId) {
		Plan plan = findOwnedPlan(userId, planId);
		List<PlanWeekAllocation> allAllocations = planWeekAllocationRepository.findAllByPlanIdOrderByBudgetWeekWeekSequenceAsc(planId);
		List<PlanWeekAllocation> yearAllocations = filterYearAllocations(allAllocations);

		return new PlanDetailResponse(
			plan.getId(),
			plan.getTitle(),
			plan.getDescription(),
			plan.getPriority(),
			plan.getTargetAmount(),
			plan.getSavedAmount(),
			calculateAchievementRate(plan),
			ChronoUnit.DAYS.between(today(), plan.getEndDate()),
			new PlanDetailResponse.GraphData(
				yearAllocations.stream().map(allocation -> allocation.getBudgetWeek().getWeekSequence()).toList(),
				yearAllocations.stream().map(PlanWeekAllocation::getPlannedAmount).toList(),
				yearAllocations.stream().map(PlanWeekAllocation::getFinalAmount).toList()
			),
			yearAllocations.stream()
				.map(allocation -> new PlanDetailResponse.WeeklyPlan(
					allocation.getBudgetWeek().getWeekSequence(),
					allocation.getPlannedAmount()
				))
				.toList(),
			resolveCurrentWeek(plan.getUser()),
			yearAllocations.size(),
			calculateCompletionProjection(plan, allAllocations)
		);
	}

	public PlanWeeklyPlansPageResponse listPlanWeeklyPlans(Integer userId, Integer planId, Integer page, Integer size) {
		validatePageRequest(page, size);
		Plan plan = findOwnedPlan(userId, planId);
		List<PlanWeekAllocation> yearAllocations = filterYearAllocations(
			planWeekAllocationRepository.findAllByPlanIdOrderByBudgetWeekWeekSequenceAsc(plan.getId())
		);

		int resolvedPage = page == null ? 0 : page;
		int resolvedSize = size == null ? 3 : size;
		int fromIndex = Math.min(resolvedPage * resolvedSize, yearAllocations.size());
		int toIndex = Math.min(fromIndex + resolvedSize, yearAllocations.size());
		List<PlanWeeklyPlansPageResponse.WeeklyPlan> weeklyPlans = yearAllocations.subList(fromIndex, toIndex).stream()
			.map(allocation -> new PlanWeeklyPlansPageResponse.WeeklyPlan(
				allocation.getBudgetWeek().getWeekSequence(),
				allocation.getPlannedAmount()
			))
			.toList();
		int totalPages = yearAllocations.isEmpty() ? 0 : (int)Math.ceil((double)yearAllocations.size() / resolvedSize);

		return new PlanWeeklyPlansPageResponse(
			planId,
			weeklyPlans,
			resolvedPage,
			resolvedSize,
			yearAllocations.size(),
			totalPages,
			(resolvedPage + 1) * resolvedSize < yearAllocations.size()
		);
	}

	private void validatePageRequest(Integer page, Integer size) {
		if (page != null && page < 0) {
			throw new PlanException(PlanErrorCode.INVALID_PAGE_REQUEST);
		}
		if (size != null && size <= 0) {
			throw new PlanException(PlanErrorCode.INVALID_PAGE_REQUEST);
		}
	}

	private Plan findOwnedPlan(Integer userId, Integer planId) {
		return planRepository.findByIdAndUserIdAndDeletedFalse(planId, userId)
			.orElseThrow(() -> new PlanException(PlanErrorCode.PLAN_NOT_FOUND));
	}

	private List<PlanWeekAllocation> filterYearAllocations(List<PlanWeekAllocation> allocations) {
		LocalDate yearStart = LocalDate.of(today().getYear(), 1, 1);
		LocalDate yearEnd = LocalDate.of(today().getYear(), 12, 31);
		return allocations.stream()
			.filter(allocation -> isOverlappingYear(allocation.getBudgetWeek(), yearStart, yearEnd))
			.toList();
	}

	private boolean isOverlappingYear(BudgetWeek budgetWeek, LocalDate yearStart, LocalDate yearEnd) {
		return !budgetWeek.getWeekEndDate().isBefore(yearStart) && !budgetWeek.getWeekStartDate().isAfter(yearEnd);
	}

	private int resolveCurrentWeek(User user) {
		return budgetWeekRepository.findFirstByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualOrderByIdDesc(
			user.getId(),
			today(),
			today()
		)
			.map(BudgetWeek::getWeekSequence)
			.orElse(0);
	}

	private int calculateAchievementRate(Plan plan) {
		if (plan.getTargetAmount() == null || plan.getTargetAmount() == 0L) {
			return 0;
		}
		return (int)((plan.getSavedAmount() * 100) / plan.getTargetAmount());
	}

	private PlanDetailResponse.CompletionProjection calculateCompletionProjection(Plan plan, List<PlanWeekAllocation> allocations) {
		long remainingAmount = Math.max(0L, zero(plan.getTargetAmount()) - zero(plan.getSavedAmount()));
		if (remainingAmount == 0L) {
			return new PlanDetailResponse.CompletionProjection(0, today());
		}

		long cumulativeAmount = 0L;
		int weeksToGoal = 0;
		LocalDate estimatedCompletionDate = null;

		for (PlanWeekAllocation allocation : allocations) {
			if (allocation.getBudgetWeek() == null) {
				continue;
			}
			if (allocation.getBudgetWeek().getWeekEndDate().isBefore(today())) {
				continue;
			}

			long finalAmount = zero(allocation.getFinalAmount());
			if (finalAmount <= 0L) {
				continue;
			}

			cumulativeAmount += finalAmount;
			weeksToGoal++;
			if (cumulativeAmount >= remainingAmount) {
				estimatedCompletionDate = allocation.getBudgetWeek().getWeekEndDate();
				break;
			}
		}

		if (estimatedCompletionDate == null) {
			return new PlanDetailResponse.CompletionProjection(null, null);
		}

		return new PlanDetailResponse.CompletionProjection(weeksToGoal, estimatedCompletionDate);
	}

	private long zero(Long amount) {
		return amount == null ? 0L : amount;
	}

	private LocalDate today() {
		return LocalDate.now(clock);
	}
}

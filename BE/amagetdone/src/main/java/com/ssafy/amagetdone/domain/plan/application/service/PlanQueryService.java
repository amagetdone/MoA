package com.ssafy.amagetdone.domain.plan.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetAdjustmentType;
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
	private final BudgetAdjustmentRepository budgetAdjustmentRepository;
	private final BudgetWeekRepository budgetWeekRepository;
	private final Clock clock;

	public PlanQueryService(
		PlanRepository planRepository,
		PlanWeekAllocationRepository planWeekAllocationRepository,
		BudgetAdjustmentRepository budgetAdjustmentRepository,
		BudgetWeekRepository budgetWeekRepository,
		Clock clock
	) {
		this.planRepository = planRepository;
		this.planWeekAllocationRepository = planWeekAllocationRepository;
		this.budgetAdjustmentRepository = budgetAdjustmentRepository;
		this.budgetWeekRepository = budgetWeekRepository;
		this.clock = clock;
	}

	public PlanDetailResponse getPlanDetail(Integer userId, Integer planId) {
		Plan plan = findOwnedPlan(userId, planId);
		List<PlanWeekAllocation> allAllocations = planWeekAllocationRepository.findAllByPlanIdOrderByBudgetWeekWeekSequenceAsc(planId);
		List<PlanWeekAllocation> yearAllocations = filterYearAllocations(allAllocations);
		List<DisplayAllocation> displayAllocations = toDisplayAllocations(yearAllocations);

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
				displayAllocations.stream().map(DisplayAllocation::displayWeek).toList(),
				displayAllocations.stream().map(displayAllocation -> zero(displayAllocation.allocation().getPlannedAmount())).toList(),
				displayAllocations.stream().map(DisplayAllocation::actualAmount).toList()
			),
			displayAllocations.stream()
				.map(displayAllocation -> new PlanDetailResponse.WeeklyPlan(
					displayAllocation.displayWeek(),
					zero(displayAllocation.allocation().getPlannedAmount())
				))
				.toList(),
			resolveCurrentWeek(plan.getUser(), displayAllocations),
			displayAllocations.size(),
			calculateCompletionProjection(plan, allAllocations)
		);
	}

	public PlanWeeklyPlansPageResponse listPlanWeeklyPlans(Integer userId, Integer planId, Integer page, Integer size) {
		validatePageRequest(page, size);
		Plan plan = findOwnedPlan(userId, planId);
		List<DisplayAllocation> displayAllocations = toDisplayAllocations(filterYearAllocations(
			planWeekAllocationRepository.findAllByPlanIdOrderByBudgetWeekWeekSequenceAsc(plan.getId())
		));

		int resolvedPage = page == null ? 0 : page;
		int resolvedSize = size == null ? 3 : size;
		int fromIndex = Math.min(resolvedPage * resolvedSize, displayAllocations.size());
		int toIndex = Math.min(fromIndex + resolvedSize, displayAllocations.size());
		List<PlanWeeklyPlansPageResponse.WeeklyPlan> weeklyPlans = displayAllocations.subList(fromIndex, toIndex).stream()
			.map(displayAllocation -> new PlanWeeklyPlansPageResponse.WeeklyPlan(
				displayAllocation.displayWeek(),
				zero(displayAllocation.allocation().getPlannedAmount())
			))
			.toList();
		int totalPages = displayAllocations.isEmpty() ? 0 : (int)Math.ceil((double)displayAllocations.size() / resolvedSize);

		return new PlanWeeklyPlansPageResponse(
			planId,
			weeklyPlans,
			resolvedPage,
			resolvedSize,
			displayAllocations.size(),
			totalPages,
			(resolvedPage + 1) * resolvedSize < displayAllocations.size()
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

	private int resolveCurrentWeek(User user, List<DisplayAllocation> displayAllocations) {
		BudgetWeek currentWeek = budgetWeekRepository.findFirstByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualOrderByIdDesc(
			user.getId(),
			today(),
			today()
		)
			.orElse(null);
		if (currentWeek == null) {
			return 0;
		}

		return displayAllocations.stream()
			.filter(displayAllocation -> isSameBudgetWeek(displayAllocation.allocation().getBudgetWeek(), currentWeek))
			.map(DisplayAllocation::displayWeek)
			.findFirst()
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

	private List<DisplayAllocation> toDisplayAllocations(List<PlanWeekAllocation> allocations) {
		Map<AdjustmentKey, Long> actualAmountsByKey = resolveActualAmountsByKey(allocations);
		List<DisplayAllocation> displayAllocations = new ArrayList<>();
		LocalDate today = today();

		for (int index = 0; index < allocations.size(); index++) {
			PlanWeekAllocation allocation = allocations.get(index);
			long actualAmount = isFutureWeek(allocation.getBudgetWeek(), today)
				? 0L
				: actualAmountsByKey.getOrDefault(AdjustmentKey.from(allocation), 0L);
			displayAllocations.add(new DisplayAllocation(index + 1, allocation, actualAmount));
		}

		return displayAllocations;
	}

	private Map<AdjustmentKey, Long> resolveActualAmountsByKey(List<PlanWeekAllocation> allocations) {
		List<BudgetWeek> budgetWeeks = allocations.stream()
			.map(PlanWeekAllocation::getBudgetWeek)
			.filter(Objects::nonNull)
			.distinct()
			.toList();
		if (budgetWeeks.isEmpty()) {
			return Map.of();
		}

		Map<AdjustmentKey, Long> actualAmountsByKey = new LinkedHashMap<>();
		for (BudgetAdjustment adjustment : budgetAdjustmentRepository.findAllByBudgetWeekIn(budgetWeeks)) {
			if (!isActualSavingAdjustment(adjustment) || adjustment.getPlan() == null || adjustment.getBudgetWeek() == null) {
				continue;
			}
			actualAmountsByKey.merge(
				AdjustmentKey.from(adjustment),
				signedActualAmount(adjustment),
				Long::sum
			);
		}
		return actualAmountsByKey;
	}

	private boolean isActualSavingAdjustment(BudgetAdjustment adjustment) {
		return adjustment.getAdjustmentType() == BudgetAdjustmentType.PLAN_SAVED_ADD
			|| adjustment.getAdjustmentType() == BudgetAdjustmentType.PLAN_SAVED_AUTO_ADD
			|| adjustment.getAdjustmentType() == BudgetAdjustmentType.PLAN_SAVED_AUTO_SUBTRACT;
	}

	private long signedActualAmount(BudgetAdjustment adjustment) {
		return switch (adjustment.getAdjustmentType()) {
			case PLAN_SAVED_ADD, PLAN_SAVED_AUTO_ADD -> zero(adjustment.getAdjustmentAmount());
			case PLAN_SAVED_AUTO_SUBTRACT -> -zero(adjustment.getAdjustmentAmount());
			default -> 0L;
		};
	}

	private boolean isFutureWeek(BudgetWeek budgetWeek, LocalDate today) {
		return budgetWeek == null || budgetWeek.getWeekStartDate() == null || budgetWeek.getWeekStartDate().isAfter(today);
	}

	private boolean isSameBudgetWeek(BudgetWeek left, BudgetWeek right) {
		if (left == null || right == null) {
			return false;
		}
		return Objects.equals(left.getWeekStartDate(), right.getWeekStartDate())
			&& Objects.equals(left.getWeekEndDate(), right.getWeekEndDate());
	}

	private LocalDate today() {
		return LocalDate.now(clock);
	}

	private record DisplayAllocation(int displayWeek, PlanWeekAllocation allocation, long actualAmount) {
	}

	private record AdjustmentKey(Integer planId, LocalDate weekStartDate, LocalDate weekEndDate) {
		private static AdjustmentKey from(PlanWeekAllocation allocation) {
			BudgetWeek budgetWeek = allocation.getBudgetWeek();
			return new AdjustmentKey(
				allocation.getPlan().getId(),
				budgetWeek == null ? null : budgetWeek.getWeekStartDate(),
				budgetWeek == null ? null : budgetWeek.getWeekEndDate()
			);
		}

		private static AdjustmentKey from(BudgetAdjustment adjustment) {
			BudgetWeek budgetWeek = adjustment.getBudgetWeek();
			return new AdjustmentKey(
				adjustment.getPlan().getId(),
				budgetWeek == null ? null : budgetWeek.getWeekStartDate(),
				budgetWeek == null ? null : budgetWeek.getWeekEndDate()
			);
		}
	}
}

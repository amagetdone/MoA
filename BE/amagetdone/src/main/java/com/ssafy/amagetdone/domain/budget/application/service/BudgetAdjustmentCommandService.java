package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetAdjustmentDto;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetAdjustmentType;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetWeekStatus;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BudgetAdjustmentCommandService {

	private static final Pattern YEAR_WEEK_PATTERN = Pattern.compile("^(\\d{4})-W(\\d{2})$");
	private static final WeekFields ISO_WEEK_FIELDS = WeekFields.of(Locale.UK);

	private final BudgetWeekRepository budgetWeekRepository;
	private final PlanWeekAllocationRepository planWeekAllocationRepository;
	private final BudgetAdjustmentRepository budgetAdjustmentRepository;
	private final EntityManager entityManager;

	public BudgetAdjustmentDto.Response saveAdjustment(Integer userId, String yearWeek, BudgetAdjustmentDto.Request request) {
		validateRequest(request);

		LocalDate weekStartDate = parseWeekStartDate(yearWeek);
		User userReference = entityManager.getReference(User.class, userId);
		BudgetWeek budgetWeek = budgetWeekRepository
			.findByUserAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(userReference, weekStartDate, weekStartDate)
			.orElseThrow(() -> new IllegalArgumentException("Budget week not found"));

		if (budgetWeek.getStatus() == BudgetWeekStatus.CLOSED) {
			throw new IllegalArgumentException("Closed budget weeks cannot be adjusted");
		}

		List<PlanWeekAllocation> planAllocations = planWeekAllocationRepository.findByBudgetWeek(budgetWeek);
		BudgetAdjustmentType adjustmentType = request.type();
		long adjustmentAmount = request.amount();
		PlanWeekAllocation targetAllocation = null;

		switch (adjustmentType) {
			case FREE_BUDGET_ADD -> budgetWeek.setManualAdjustmentAmount(
				zero(budgetWeek.getManualAdjustmentAmount()) + adjustmentAmount
			);
			case FREE_BUDGET_SUBTRACT -> budgetWeek.setManualAdjustmentAmount(
				zero(budgetWeek.getManualAdjustmentAmount()) - adjustmentAmount
			);
			case PLAN_ADD -> {
				targetAllocation = requirePlanAllocation(userId, request.planId(), planAllocations);
				targetAllocation.setFinalAmount(zero(targetAllocation.getFinalAmount()) + adjustmentAmount);
			}
			case PLAN_SUBTRACT -> {
				targetAllocation = requirePlanAllocation(userId, request.planId(), planAllocations);
				long currentFinalAmount = zero(targetAllocation.getFinalAmount());
				if (currentFinalAmount < adjustmentAmount) {
					throw new IllegalArgumentException("Adjustment amount exceeds plan final amount");
				}
				targetAllocation.setFinalAmount(currentFinalAmount - adjustmentAmount);
			}
			case PLAN_SAVED_ADD -> {
				targetAllocation = requirePlanAllocation(userId, request.planId(), planAllocations);
				long remainingFreeBudget = calculateRemainingFreeBudget(budgetWeek);
				if (remainingFreeBudget < adjustmentAmount) {
					throw new IllegalArgumentException("Adjustment amount exceeds remaining free budget");
				}
				targetAllocation.getPlan().setSavedAmount(
					zero(targetAllocation.getPlan().getSavedAmount()) + adjustmentAmount
				);
				budgetWeek.setFinalFreeAmount(zero(budgetWeek.getFinalFreeAmount()) - adjustmentAmount);
			}
			case PLAN_DELAY, PLAN_EARLY_CLOSE, ROLLOVER_APPLIED -> throw new IllegalArgumentException(
				"Unsupported adjustment type: " + adjustmentType
			);
		}

		budgetWeek.setStatus(BudgetWeekStatus.ADJUSTED);
		if (adjustmentType != BudgetAdjustmentType.PLAN_SAVED_ADD) {
			budgetWeek.setFinalFreeAmount(calculateFinalFreeAmount(budgetWeek, planAllocations));
		}

		BudgetAdjustment savedAdjustment = budgetAdjustmentRepository.save(BudgetAdjustment.builder()
			.budgetWeek(budgetWeek)
			.plan(targetAllocation == null ? null : targetAllocation.getPlan())
			.adjustmentType(adjustmentType)
			.adjustmentAmount(adjustmentAmount)
			.reason(request.reason().trim())
			.build());

		budgetWeekRepository.save(budgetWeek);

		return new BudgetAdjustmentDto.Response(
			yearWeek,
			savedAdjustment.getAdjustmentType().name(),
			savedAdjustment.getAdjustmentAmount(),
			targetAllocation == null ? null : targetAllocation.getPlan().getId(),
			savedAdjustment.getReason(),
			budgetWeek.getManualAdjustmentAmount(),
			targetAllocation == null ? null : targetAllocation.getFinalAmount(),
			budgetWeek.getFinalFreeAmount(),
			budgetWeek.getStatus().name()
		);
	}

	private void validateRequest(BudgetAdjustmentDto.Request request) {
		if (request == null) {
			throw new IllegalArgumentException("Adjustment request is required");
		}
		if (request.type() == null) {
			throw new IllegalArgumentException("Adjustment type is required");
		}
		if (request.amount() == null || request.amount() <= 0L) {
			throw new IllegalArgumentException("Adjustment amount must be greater than zero");
		}
		if (request.reason() == null || request.reason().isBlank()) {
			throw new IllegalArgumentException("Adjustment reason is required");
		}
		if (request.reason().length() > 255) {
			throw new IllegalArgumentException("Adjustment reason must be 255 characters or fewer");
		}
		if (requiresPlan(request.type()) && request.planId() == null) {
			throw new IllegalArgumentException("planId is required for plan adjustments");
		}
	}

	private boolean requiresPlan(BudgetAdjustmentType adjustmentType) {
		return adjustmentType == BudgetAdjustmentType.PLAN_ADD
			|| adjustmentType == BudgetAdjustmentType.PLAN_SUBTRACT
			|| adjustmentType == BudgetAdjustmentType.PLAN_SAVED_ADD;
	}

	private PlanWeekAllocation requirePlanAllocation(
		Integer userId,
		Integer planId,
		List<PlanWeekAllocation> planAllocations
	) {
		return planAllocations.stream()
			.filter(allocation -> allocation.getPlan().getId().equals(planId))
			.filter(allocation -> allocation.getPlan().getUser().getId().equals(userId))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Plan allocation not found"));
	}

	private long calculateFinalFreeAmount(BudgetWeek budgetWeek, List<PlanWeekAllocation> planAllocations) {
		long finalSavingAmount = planAllocations.stream()
			.mapToLong(allocation -> zero(allocation.getFinalAmount()))
			.sum();

		return zero(budgetWeek.getPlannedIncomeAmount())
			- zero(budgetWeek.getPlannedEssentialAmount())
			- finalSavingAmount
			- zero(budgetWeek.getRolloverInAmount())
			+ zero(budgetWeek.getManualAdjustmentAmount());
	}

	private long calculateRemainingFreeBudget(BudgetWeek budgetWeek) {
		return zero(budgetWeek.getFinalFreeAmount()) - zero(budgetWeek.getActualSpentAmount());
	}

	private long zero(Long amount) {
		return amount == null ? 0L : amount;
	}

	private LocalDate parseWeekStartDate(String yearWeek) {
		Matcher matcher = YEAR_WEEK_PATTERN.matcher(yearWeek);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Invalid yearWeek: " + yearWeek);
		}

		try {
			int weekBasedYear = Integer.parseInt(matcher.group(1));
			int weekOfYear = Integer.parseInt(matcher.group(2));
			return LocalDate.of(weekBasedYear, 1, 4)
				.with(ISO_WEEK_FIELDS.weekOfWeekBasedYear(), weekOfYear)
				.with(ISO_WEEK_FIELDS.dayOfWeek(), 1);
		} catch (DateTimeException exception) {
			throw new IllegalArgumentException("Invalid yearWeek: " + yearWeek, exception);
		}
	}
}

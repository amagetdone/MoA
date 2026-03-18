package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ssafy.amagetdone.domain.budget.api.dto.WeeklySavingsDetailDto;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WeeklySavingsDetailQueryService {

	private static final Pattern YEAR_WEEK_PATTERN = Pattern.compile("^(\\d{4})-W(\\d{2})$");
	private static final WeekFields ISO_WEEK_FIELDS = WeekFields.of(Locale.UK);

	private final BudgetWeekRepository budgetWeekRepository;
	private final PlanWeekAllocationRepository planWeekAllocationRepository;
	private final BudgetAdjustmentRepository budgetAdjustmentRepository;
	private final EntityManager entityManager;

	public WeeklySavingsDetailDto.Response getDetail(Integer userId, String yearWeek) {
		LocalDate weekStartDate = parseWeekStartDate(yearWeek);
		LocalDate weekEndDate = weekStartDate.plusDays(6L);
		User userReference = entityManager.getReference(User.class, userId);
		BudgetWeek budgetWeek = budgetWeekRepository.findByUserAndWeekStartDateBetweenOrderByWeekSequence(
			userReference,
			weekStartDate,
			weekEndDate
		)
			.stream()
			.filter(candidate -> !candidate.getWeekEndDate().isBefore(weekStartDate))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Budget week not found"));

		List<PlanWeekAllocation> allocations = planWeekAllocationRepository.findByBudgetWeek(budgetWeek);
		Map<Integer, WeeklySavingsDetailDto.AdjustmentSummary> adjustmentsByPlanId = summarizeAdjustments(
			budgetWeek,
			allocations
		);

		long finalSavingAmount = allocations.stream()
			.mapToLong(allocation -> zero(allocation.getFinalAmount()))
			.sum();

		return WeeklySavingsDetailDto.Response.of(
			yearWeek,
			budgetWeek.getWeekStartDate(),
			budgetWeek.getWeekEndDate(),
			budgetWeek.getPlannedSavingAmount(),
			finalSavingAmount,
			toSavingDetails(allocations, adjustmentsByPlanId)
		);
	}

	private List<WeeklySavingsDetailDto.SavingDetail> toSavingDetails(
		List<PlanWeekAllocation> allocations,
		Map<Integer, WeeklySavingsDetailDto.AdjustmentSummary> adjustmentsByPlanId
	) {
		return allocations.stream()
			.sorted(Comparator.comparing((PlanWeekAllocation allocation) -> allocation.getPlan().getPriority())
				.thenComparing(allocation -> allocation.getPlan().getTitle()))
			.map(allocation -> {
				Plan plan = allocation.getPlan();
				return new WeeklySavingsDetailDto.SavingDetail(
					plan.getTitle(),
					plan.getPriority(),
					zero(allocation.getPlannedAmount()),
					zero(allocation.getFinalAmount()),
					zero(plan.getSavedAmount()),
					adjustmentsByPlanId.getOrDefault(plan.getId(), WeeklySavingsDetailDto.AdjustmentSummary.none())
				);
			})
			.toList();
	}

	private Map<Integer, WeeklySavingsDetailDto.AdjustmentSummary> summarizeAdjustments(
		BudgetWeek budgetWeek,
		List<PlanWeekAllocation> allocations
	) {
		Set<Integer> planIds = new HashSet<>();
		for (PlanWeekAllocation allocation : allocations) {
			planIds.add(allocation.getPlan().getId());
		}

		Map<Integer, List<BudgetAdjustment>> groupedAdjustments = new HashMap<>();
		for (BudgetAdjustment adjustment : budgetAdjustmentRepository.findByBudgetWeekOrderByCreatedAtDesc(budgetWeek)) {
			if (adjustment.getPlan() == null || !planIds.contains(adjustment.getPlan().getId())) {
				continue;
			}
			groupedAdjustments.computeIfAbsent(adjustment.getPlan().getId(), ignored -> new java.util.ArrayList<>())
				.add(adjustment);
		}

		Map<Integer, WeeklySavingsDetailDto.AdjustmentSummary> summaries = new HashMap<>();
		for (Map.Entry<Integer, List<BudgetAdjustment>> entry : groupedAdjustments.entrySet()) {
			BudgetAdjustment latestAdjustment = entry.getValue().get(0);
			summaries.put(
				entry.getKey(),
				new WeeklySavingsDetailDto.AdjustmentSummary(
					entry.getValue().size(),
					latestAdjustment.getAdjustmentType().name(),
					latestAdjustment.getReason()
				)
			);
		}

		return summaries;
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

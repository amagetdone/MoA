package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetWeekSetupDto;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialWeekAllocation;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialWeekAllocationRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BudgetWeekSetupQueryService {

	private static final Pattern YEAR_WEEK_PATTERN = Pattern.compile("^(\\d{4})-W(\\d{2})$");
	private static final WeekFields ISO_WEEK_FIELDS = WeekFields.of(Locale.UK);

	private final BudgetWeekRepository budgetWeekRepository;
	private final EssentialWeekAllocationRepository essentialWeekAllocationRepository;
	private final PlanWeekAllocationRepository planWeekAllocationRepository;
	private final EntityManager entityManager;

	public BudgetWeekSetupDto.Response getSetup(Integer userId, String yearWeek) {
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

		return BudgetWeekSetupDto.Response.of(
			budgetWeek.getPlannedIncomeAmount(),
			budgetWeek.getPlannedEssentialAmount(),
			budgetWeek.getPlannedSavingAmount(),
			budgetWeek.getFinalFreeAmount(),
			budgetWeek.getActualSpentAmount(),
			toEssentials(essentialWeekAllocationRepository.findByBudgetWeek(budgetWeek)),
			toSavings(planWeekAllocationRepository.findByBudgetWeek(budgetWeek))
		);
	}

	private List<BudgetWeekSetupDto.Essential> toEssentials(List<EssentialWeekAllocation> allocations) {
		return allocations.stream()
			.sorted(Comparator.comparing(EssentialWeekAllocation::getExpenseScope)
				.thenComparing(allocation -> allocation.getCategory().getId()))
			.map(allocation -> new BudgetWeekSetupDto.Essential(
				allocation.getCategory().getId(),
				allocation.getCategory().getName(),
				allocation.getExpenseScope().name(),
				allocation.getPlannedAmount(),
				allocation.getActualSpentAmount()
			))
			.toList();
	}

	private List<BudgetWeekSetupDto.Saving> toSavings(List<PlanWeekAllocation> allocations) {
		return allocations.stream()
			.sorted(Comparator.comparing((PlanWeekAllocation allocation) -> allocation.getPlan().getPriority())
				.thenComparing(allocation -> allocation.getPlan().getTitle()))
			.map(allocation -> new BudgetWeekSetupDto.Saving(
				allocation.getPlan().getTitle(),
				allocation.getPlan().getPriority(),
				allocation.getPlannedAmount(),
				allocation.getFinalAmount()
			))
			.toList();
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

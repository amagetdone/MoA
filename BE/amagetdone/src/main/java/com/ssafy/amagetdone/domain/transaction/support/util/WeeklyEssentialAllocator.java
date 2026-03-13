package com.ssafy.amagetdone.domain.transaction.support.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.ssafy.amagetdone.domain.asset.model.entity.Category;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;

import org.springframework.stereotype.Component;

@Component
public class WeeklyEssentialAllocator {

	private static final WeekFields ISO_WEEK_FIELDS = WeekFields.of(Locale.UK);

	public List<WeeklySnapshot> allocate(int baseMonths, List<Entry> entries, LocalDateTime analyzedAt) {
		Map<WeekKey, List<Entry>> weeklyEntries = entries.stream()
			.collect(Collectors.groupingBy(entry -> WeekKey.from(entry.occurredDate())));

		return weeklyEntries.entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.map(entry -> toSnapshot(entry.getKey(), entry.getValue(), baseMonths, analyzedAt))
			.toList();
	}

	private WeeklySnapshot toSnapshot(WeekKey weekKey, List<Entry> entries, int baseMonths, LocalDateTime analyzedAt) {
		long fixedAmount = entries.stream()
			.filter(entry -> entry.expenseScope() == ExpenseScope.FIXED)
			.mapToLong(Entry::amount)
			.sum();
		long variableAmount = entries.stream()
			.filter(entry -> entry.expenseScope() == ExpenseScope.VARIABLE)
			.mapToLong(Entry::amount)
			.sum();
		List<CategoryAllocation> categoryAllocations = entries.stream()
			.collect(Collectors.groupingBy(
				entry -> new CategoryScopeKey(entry.category(), entry.expenseScope()),
				Collectors.summingLong(Entry::amount)
			))
			.entrySet().stream()
			.map(entry -> new CategoryAllocation(
				entry.getKey().category(),
				entry.getKey().expenseScope(),
				entry.getValue()
			))
			.toList();

		return new WeeklySnapshot(
			weekKey.yearWeek(),
			weekKey.weekStartDate(),
			weekKey.weekEndDate(),
			baseMonths,
			fixedAmount + variableAmount,
			fixedAmount,
			variableAmount,
			analyzedAt,
			categoryAllocations
		);
	}

	public record Entry(
		Category category,
		ExpenseScope expenseScope,
		LocalDate occurredDate,
		Long amount
	) {
	}

	public record CategoryAllocation(
		Category category,
		ExpenseScope expenseScope,
		Long amount
	) {
	}

	public record WeeklySnapshot(
		String yearWeek,
		LocalDate weekStartDate,
		LocalDate weekEndDate,
		Integer baseMonths,
		Long essentialAmount,
		Long fixedEssentialAmount,
		Long variableEssentialAmount,
		LocalDateTime analyzedAt,
		List<CategoryAllocation> categoryAllocations
	) {
	}

	private record WeekKey(
		String yearWeek,
		LocalDate weekStartDate,
		LocalDate weekEndDate
	) implements Comparable<WeekKey> {

		private static WeekKey from(LocalDate occurredDate) {
			LocalDate weekStartDate = occurredDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
			LocalDate weekEndDate = occurredDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
			int weekBasedYear = occurredDate.get(ISO_WEEK_FIELDS.weekBasedYear());
			int weekOfYear = occurredDate.get(ISO_WEEK_FIELDS.weekOfWeekBasedYear());
			return new WeekKey(
				"%d-W%02d".formatted(weekBasedYear, weekOfYear),
				weekStartDate,
				weekEndDate
			);
		}

		@Override
		public int compareTo(WeekKey other) {
			return weekStartDate.compareTo(other.weekStartDate);
		}
	}

	private record CategoryScopeKey(
		Category category,
		ExpenseScope expenseScope
	) {
	}
}

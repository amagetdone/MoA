package com.ssafy.amagetdone.domain.plan.application.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class PlanWeekAllocationCalculator {

	public List<WeekAmount> calculate(LocalDate startDate, LocalDate endDate, Long targetAmount) {
		List<WeekAmount> weeks = new ArrayList<>();
		LocalDate weekStart = startDate.with(DayOfWeek.MONDAY);
		while (!weekStart.isAfter(endDate)) {
			LocalDate weekEnd = weekStart.plusDays(6);
			if (!weekEnd.isBefore(startDate)) {
				weeks.add(new WeekAmount(weekStart, weekEnd, 0L));
			}
			weekStart = weekStart.plusWeeks(1);
		}

		long baseAmount = targetAmount / weeks.size();
		long remainder = targetAmount % weeks.size();
		List<WeekAmount> result = new ArrayList<>(weeks.size());
		for (int i = 0; i < weeks.size(); i++) {
			long amount = baseAmount;
			if (i == weeks.size() - 1) {
				amount += remainder;
			}
			WeekAmount week = weeks.get(i);
			result.add(new WeekAmount(week.weekStartDate(), week.weekEndDate(), amount));
		}
		return result;
	}

	public record WeekAmount(
		LocalDate weekStartDate,
		LocalDate weekEndDate,
		Long amount
	) {
	}
}

package com.ssafy.amagetdone.domain.plan.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class PlanWeekAllocationCalculatorTest {

	private final PlanWeekAllocationCalculator calculator = new PlanWeekAllocationCalculator();

	@Test
	void createsCalendarWeekSlotsThatOverlapWithPlanPeriod() {
		List<PlanWeekAllocationCalculator.WeekAmount> allocations = calculator.calculate(
			LocalDate.of(2026, 4, 1),
			LocalDate.of(2026, 4, 14),
			300L
		);

		assertEquals(3, allocations.size());
		assertEquals(LocalDate.of(2026, 3, 30), allocations.get(0).weekStartDate());
		assertEquals(LocalDate.of(2026, 4, 13), allocations.get(2).weekStartDate());
	}

	@Test
	void assignsRemainderToLastWeek() {
		List<PlanWeekAllocationCalculator.WeekAmount> allocations = calculator.calculate(
			LocalDate.of(2026, 4, 1),
			LocalDate.of(2026, 4, 14),
			100L
		);

		assertEquals(List.of(33L, 33L, 34L), allocations.stream().map(PlanWeekAllocationCalculator.WeekAmount::amount).toList());
	}
}

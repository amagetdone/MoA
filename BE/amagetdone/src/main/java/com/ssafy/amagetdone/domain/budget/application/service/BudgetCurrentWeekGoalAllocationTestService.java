package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;

import com.ssafy.amagetdone.domain.budget.api.dto.WeeklySavingsDetailDto;
import com.ssafy.amagetdone.domain.budget.application.usecase.TriggerBudgetAllocationUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BudgetCurrentWeekGoalAllocationTestService {

	private static final WeekFields ISO_WEEK_FIELDS = WeekFields.of(Locale.UK);

	private final TriggerBudgetAllocationUseCase triggerBudgetAllocationUseCase;
	private final WeeklySavingsDetailQueryService weeklySavingsDetailQueryService;
	private final Clock clock;

	public WeeklySavingsDetailDto.Response forceCurrentWeekAllocation(Integer userId) {
		triggerBudgetAllocationUseCase.execute(userId);

		LocalDate today = LocalDate.now(clock);
		String currentYearWeek = formatYearWeek(today);
		return weeklySavingsDetailQueryService.getDetail(userId, currentYearWeek);
	}

	private String formatYearWeek(LocalDate date) {
		int weekBasedYear = date.get(ISO_WEEK_FIELDS.weekBasedYear());
		int weekOfYear = date.get(ISO_WEEK_FIELDS.weekOfWeekBasedYear());
		return "%d-W%02d".formatted(weekBasedYear, weekOfYear);
	}
}

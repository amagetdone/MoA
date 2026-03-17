package com.ssafy.amagetdone.domain.budget.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetWeekSeedRequest;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetWeekSeedResponse;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BudgetWeekSeedServiceTest {

	@Test
	void seedCreatesMondayToSundayWeeksForRequestedRange() {
		BudgetWeekRepository budgetWeekRepository = org.mockito.Mockito.mock(BudgetWeekRepository.class);
		UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
		BudgetWeekSeedService service = new BudgetWeekSeedService(budgetWeekRepository, userRepository);

		User user = org.mockito.Mockito.mock(User.class);
		given(userRepository.findById(1)).willReturn(Optional.of(user));
		given(budgetWeekRepository.findTopByOrderByIdDesc()).willReturn(Optional.empty());
		given(budgetWeekRepository.findTopByUserIdOrderByWeekSequenceDesc(1)).willReturn(Optional.empty());

		BudgetWeekSeedResponse response = service.seed(1, new BudgetWeekSeedRequest(
			LocalDate.of(2026, 3, 16),
			LocalDate.of(2026, 3, 31)
		));

		verify(budgetWeekRepository).deleteAllByUserIdAndWeekEndDateGreaterThanEqualAndWeekStartDateLessThanEqual(
			eq(1),
			eq(LocalDate.of(2026, 3, 16)),
			eq(LocalDate.of(2026, 4, 5))
		);

		ArgumentCaptor<List<BudgetWeek>> captor = ArgumentCaptor.forClass(List.class);
		verify(budgetWeekRepository).saveAll(captor.capture());

		List<BudgetWeek> savedWeeks = captor.getValue();
		assertEquals(3, savedWeeks.size());
		assertEquals(LocalDate.of(2026, 3, 16), savedWeeks.get(0).getWeekStartDate());
		assertEquals(LocalDate.of(2026, 4, 5), savedWeeks.get(2).getWeekEndDate());
		assertNotNull(savedWeeks.get(0).getId());

		assertEquals(3, response.seededCount());
		assertEquals(LocalDate.of(2026, 3, 16), response.firstWeekStartDate());
		assertEquals(LocalDate.of(2026, 4, 5), response.lastWeekEndDate());
	}
}

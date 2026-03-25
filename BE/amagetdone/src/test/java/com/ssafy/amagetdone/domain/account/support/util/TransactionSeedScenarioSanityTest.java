package com.ssafy.amagetdone.domain.account.support.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.stream.Collectors;

import com.ssafy.amagetdone.domain.account.support.util.AccountRealTransactionScenario.AccountSeed;
import com.ssafy.amagetdone.domain.account.support.util.CardRealTransactionScenario.CardSeed;

import org.junit.jupiter.api.Test;

class TransactionSeedScenarioSanityTest {

	@Test
	void accountSeedsCoverExtendedMonthsAndStayNetPositive() {
		AccountRealTransactionScenario scenario = new AccountRealTransactionScenario();

		assertThat(scenario.completedMonthSeeds()).hasSizeGreaterThanOrEqualTo(75);
		assertThat(scenario.completedMonthSeeds())
			.extracting(AccountSeed::transactionDateTime)
			.map(YearMonth::from)
			.contains(YearMonth.of(2025, 12), YearMonth.of(2026, 1), YearMonth.of(2026, 2), YearMonth.of(2026, 3));
		assertThat(scenario.completedMonthSeeds())
			.extracting(AccountSeed::transactionDateTime)
			.map(dateTime -> dateTime.toLocalDate())
			.contains(LocalDate.of(2026, 3, 25));

		Map<YearMonth, Long> monthlyNetAmounts = scenario.completedMonthSeeds().stream()
			.filter(seed -> YearMonth.from(seed.transactionDateTime()).getYear() == 2026)
			.collect(Collectors.groupingBy(
				seed -> YearMonth.from(seed.transactionDateTime()),
				Collectors.summingLong(seed -> seed.transactionType() == 1
					? seed.transactionBalance()
					: -seed.transactionBalance())
			));

		assertThat(monthlyNetAmounts)
			.containsKeys(YearMonth.of(2026, 1), YearMonth.of(2026, 2), YearMonth.of(2026, 3));
		assertThat(monthlyNetAmounts.get(YearMonth.of(2026, 1))).isPositive();
		assertThat(monthlyNetAmounts.get(YearMonth.of(2026, 2))).isPositive();
		assertThat(monthlyNetAmounts.get(YearMonth.of(2026, 3))).isPositive();
	}

	@Test
	void cardSeedsCoverExtendedMonthsWithBalancedMonthlyCounts() {
		CardRealTransactionScenario scenario = new CardRealTransactionScenario();

		assertThat(scenario.completedMonthSeeds()).hasSizeGreaterThanOrEqualTo(680);
		assertThat(scenario.completedMonthSeeds())
			.extracting(CardSeed::transactionDateTime)
			.map(YearMonth::from)
			.contains(YearMonth.of(2025, 12), YearMonth.of(2026, 1), YearMonth.of(2026, 2), YearMonth.of(2026, 3));
		assertThat(scenario.completedMonthSeeds())
			.extracting(CardSeed::transactionDateTime)
			.map(dateTime -> dateTime.toLocalDate())
			.contains(LocalDate.of(2026, 3, 25));

		Map<YearMonth, Long> monthlyCounts = scenario.completedMonthSeeds().stream()
			.collect(Collectors.groupingBy(
				seed -> YearMonth.from(seed.transactionDateTime()),
				Collectors.counting()
			));

		assertThat(monthlyCounts)
			.containsKeys(YearMonth.of(2025, 12), YearMonth.of(2026, 1), YearMonth.of(2026, 2), YearMonth.of(2026, 3));
		assertThat(monthlyCounts.get(YearMonth.of(2025, 12))).isBetween(170L, 210L);
		assertThat(monthlyCounts.get(YearMonth.of(2026, 1))).isBetween(170L, 210L);
		assertThat(monthlyCounts.get(YearMonth.of(2026, 2))).isBetween(150L, 190L);
		assertThat(monthlyCounts.get(YearMonth.of(2026, 3))).isBetween(145L, 170L);
	}
}

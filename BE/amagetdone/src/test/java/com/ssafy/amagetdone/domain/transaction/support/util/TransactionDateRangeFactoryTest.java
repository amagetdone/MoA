package com.ssafy.amagetdone.domain.transaction.support.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

class TransactionDateRangeFactoryTest {

	@Test
	void resolveUsesHistoricalWindowStartWhenLatestLocalTransactionIsMoreRecent() {
		Clock clock = Clock.fixed(
			ZonedDateTime.of(2026, 3, 27, 0, 0, 0, 0, ZoneId.of("Asia/Seoul")).toInstant(),
			ZoneId.of("Asia/Seoul")
		);
		TransactionDateRangeFactory factory = new TransactionDateRangeFactory(clock);

		TransactionDateRangeFactory.DateRange range = factory.resolve(3, LocalDate.of(2026, 3, 20), false);

		assertEquals(LocalDate.of(2025, 12, 27), range.startDate());
		assertEquals(LocalDate.of(2026, 3, 27), range.endDate());
	}
}

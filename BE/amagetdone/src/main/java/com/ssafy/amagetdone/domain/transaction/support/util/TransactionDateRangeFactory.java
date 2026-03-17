package com.ssafy.amagetdone.domain.transaction.support.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

@Component
public class TransactionDateRangeFactory {

	private static final int DEFAULT_MONTHS = 6;
	private static final DateTimeFormatter BASIC_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

	private final Clock clock;

	public TransactionDateRangeFactory() {
		this(Clock.systemDefaultZone());
	}

	TransactionDateRangeFactory(Clock clock) {
		this.clock = clock;
	}

	public DateRange resolve(int months, LocalDate latestLocalDate, boolean forceFullSync) {
		LocalDate endDate = LocalDate.now(clock);
		int normalizedMonths = months > 0 ? months : DEFAULT_MONTHS;
		LocalDate startDate = forceFullSync || latestLocalDate == null
			? endDate.minusMonths(normalizedMonths)
			: latestLocalDate;
		return new DateRange(startDate, endDate);
	}

	public record DateRange(LocalDate startDate, LocalDate endDate) {

		public String startDateText() {
			return startDate.format(BASIC_DATE_FORMAT);
		}

		public String endDateText() {
			return endDate.format(BASIC_DATE_FORMAT);
		}
	}
}

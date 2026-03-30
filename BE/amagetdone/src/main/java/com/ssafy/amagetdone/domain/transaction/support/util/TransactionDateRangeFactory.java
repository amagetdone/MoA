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
		LocalDate historicalStartDate = endDate.minusMonths(normalizedMonths);
		LocalDate startDate = forceFullSync || latestLocalDate == null
			? historicalStartDate
			: latestLocalDate.isAfter(historicalStartDate) ? historicalStartDate : latestLocalDate;
		return new DateRange(startDate, endDate);
	}

	public DateRange resolve(LocalDate startDate, LocalDate endDate) {
		LocalDate resolvedEndDate = endDate == null ? LocalDate.now(clock) : endDate;
		LocalDate resolvedStartDate = startDate == null ? resolvedEndDate.minusMonths(1) : startDate;
		return new DateRange(resolvedStartDate, resolvedEndDate);
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

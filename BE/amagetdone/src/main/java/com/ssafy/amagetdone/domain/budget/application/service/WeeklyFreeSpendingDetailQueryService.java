package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyFreeSpendingDetailDto;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionListDto;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionQueryService;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WeeklyFreeSpendingDetailQueryService {

	private static final Pattern YEAR_WEEK_PATTERN = Pattern.compile("^(\\d{4})-W(\\d{2})$");
	private static final WeekFields ISO_WEEK_FIELDS = WeekFields.of(Locale.UK);

	private final BudgetWeekRepository budgetWeekRepository;
	private final TransactionQueryService transactionQueryService;
	private final EntityManager entityManager;

	public WeeklyFreeSpendingDetailDto.Response getDetail(Integer userId, String yearWeek) {
		LocalDate weekStartDate = parseWeekStartDate(yearWeek);
		User userReference = entityManager.getReference(User.class, userId);
		BudgetWeek budgetWeek = budgetWeekRepository
			.findByUserAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(userReference, weekStartDate, weekStartDate)
			.orElseThrow(() -> new IllegalArgumentException("Budget week not found"));

		long allocatedAmount = zero(budgetWeek.getFinalFreeAmount());
		long spentAmount = zero(budgetWeek.getActualSpentAmount());

		return new WeeklyFreeSpendingDetailDto.Response(
			yearWeek,
			budgetWeek.getWeekStartDate(),
			budgetWeek.getWeekEndDate(),
			allocatedAmount,
			spentAmount,
			allocatedAmount - spentAmount,
			toTransactionDetails(transactionQueryService.listNonEssentialFreeSpendingTransactions(
				userId,
				budgetWeek.getWeekStartDate(),
				budgetWeek.getWeekEndDate()
			))
		);
	}

	private List<WeeklyFreeSpendingDetailDto.TransactionDetail> toTransactionDetails(
		List<TransactionListDto.Item> expenseTransactions
	) {
		return expenseTransactions.stream()
			.map(item -> new WeeklyFreeSpendingDetailDto.TransactionDetail(
				item.transactionId(),
				item.sourceType(),
				item.sourceId(),
				item.occurredAt(),
				item.merchantText(),
				item.amount(),
				toCategoryView(item.category()),
				item.expenseType(),
				item.analysisIncluded(),
				item.excludedReason()
			))
			.toList();
	}

	private WeeklyFreeSpendingDetailDto.CategoryView toCategoryView(TransactionListDto.CategoryView categoryView) {
		if (categoryView == null) {
			return null;
		}
		return new WeeklyFreeSpendingDetailDto.CategoryView(categoryView.id(), categoryView.name());
	}

	private long zero(Long amount) {
		return amount == null ? 0L : amount;
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

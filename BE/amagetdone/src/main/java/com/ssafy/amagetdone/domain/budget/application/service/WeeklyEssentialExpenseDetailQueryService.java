package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyEssentialExpenseDetailDto;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialWeekAllocation;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialWeekAllocationRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;
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
public class WeeklyEssentialExpenseDetailQueryService {

	private static final Pattern YEAR_WEEK_PATTERN = Pattern.compile("^(\\d{4})-W(\\d{2})$");
	private static final WeekFields ISO_WEEK_FIELDS = WeekFields.of(Locale.UK);

	private final BudgetWeekRepository budgetWeekRepository;
	private final EssentialWeekAllocationRepository essentialWeekAllocationRepository;
	private final TransactionQueryService transactionQueryService;
	private final EntityManager entityManager;

	public WeeklyEssentialExpenseDetailDto.Response getDetail(Integer userId, String yearWeek) {
		LocalDate weekStartDate = parseWeekStartDate(yearWeek);
		LocalDate weekEndDate = weekStartDate.plusDays(6L);
		User userReference = entityManager.getReference(User.class, userId);
		BudgetWeek budgetWeek = budgetWeekRepository.findByUserAndWeekStartDateBetweenOrderByWeekSequence(
			userReference,
			weekStartDate,
			weekEndDate
		)
			.stream()
			.filter(candidate -> !candidate.getWeekEndDate().isBefore(weekStartDate))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Budget week not found"));

		List<EssentialWeekAllocation> allocations = essentialWeekAllocationRepository.findByBudgetWeek(budgetWeek);
		List<TransactionListDto.Item> expenseTransactions = transactionQueryService.listExpenseTransactions(
			userId,
			budgetWeek.getWeekStartDate(),
			budgetWeek.getWeekEndDate()
		);

		long plannedEssentialAmount = allocations.stream()
			.mapToLong(allocation -> zero(allocation.getPlannedAmount()))
			.sum();
		long actualSpentAmount = allocations.stream()
			.mapToLong(allocation -> zero(allocation.getActualSpentAmount()))
			.sum();

		return new WeeklyEssentialExpenseDetailDto.Response(
			yearWeek,
			budgetWeek.getWeekStartDate(),
			budgetWeek.getWeekEndDate(),
			plannedEssentialAmount,
			actualSpentAmount,
			plannedEssentialAmount - actualSpentAmount,
			toCategoryDetails(allocations, ExpenseScope.FIXED, expenseTransactions),
			toCategoryDetails(allocations, ExpenseScope.VARIABLE, expenseTransactions)
		);
	}

	private List<WeeklyEssentialExpenseDetailDto.CategoryDetail> toCategoryDetails(
		List<EssentialWeekAllocation> allocations,
		ExpenseScope expenseScope,
		List<TransactionListDto.Item> expenseTransactions
	) {
		return allocations.stream()
			.filter(allocation -> allocation.getExpenseScope() == expenseScope)
			.sorted(Comparator.comparing(allocation -> allocation.getCategory().getId()))
			.map(allocation -> {
				long plannedAmount = zero(allocation.getPlannedAmount());
				long actualSpentAmount = zero(allocation.getActualSpentAmount());
				return new WeeklyEssentialExpenseDetailDto.CategoryDetail(
					allocation.getCategory().getId(),
					allocation.getCategory().getName(),
					plannedAmount,
					actualSpentAmount,
					plannedAmount - actualSpentAmount,
					toTransactionDetails(expenseTransactions, allocation)
				);
			})
			.toList();
	}

	private List<WeeklyEssentialExpenseDetailDto.TransactionDetail> toTransactionDetails(
		List<TransactionListDto.Item> expenseTransactions,
		EssentialWeekAllocation allocation
	) {
		return expenseTransactions.stream()
			.filter(item -> item.analysisIncluded())
			.filter(item -> item.category() != null && allocation.getCategory().getId().equals(item.category().id()))
			.filter(item -> allocation.getExpenseScope().name().equals(item.expenseType()))
			.map(item -> new WeeklyEssentialExpenseDetailDto.TransactionDetail(
				item.transactionId(),
				item.sourceType(),
				item.sourceId(),
				item.occurredAt(),
				item.merchantText(),
				item.amount(),
				new WeeklyEssentialExpenseDetailDto.CategoryView(item.category().id(), item.category().name()),
				item.expenseType()
			))
			.toList();
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

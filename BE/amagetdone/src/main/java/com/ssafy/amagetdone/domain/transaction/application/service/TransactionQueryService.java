package com.ssafy.amagetdone.domain.transaction.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeekCategory;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekCategoryRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;
import com.ssafy.amagetdone.domain.transaction.api.dto.ExpenseSummaryDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisJobDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionListDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.application.usecase.SyncLatestTransactionsUseCase;
import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisResult;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.repository.AccountTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.CardTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisResultRepository;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionExcludedReason;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TransactionQueryService {

	private static final WeekFields ISO_WEEK_FIELDS = WeekFields.of(Locale.UK);

	private final AccountTransactionOverrideRepository accountTransactionOverrideRepository;
	private final CardTransactionOverrideRepository cardTransactionOverrideRepository;
	private final TransactionAnalysisResultRepository transactionAnalysisResultRepository;
	private final EssentialExpenseSnapshotWeekRepository essentialExpenseSnapshotWeekRepository;
	private final EssentialExpenseSnapshotWeekCategoryRepository essentialExpenseSnapshotWeekCategoryRepository;
	private final SyncLatestTransactionsUseCase syncLatestTransactionsUseCase;

	public ExpenseSummaryDto.Response getExpenseSummary(
		Integer userId,
		YearMonth fromMonth,
		YearMonth toMonth,
		String bucket
	) {
		YearMonth resolvedToMonth = toMonth == null ? YearMonth.now() : toMonth;
		YearMonth resolvedFromMonth = fromMonth == null ? resolvedToMonth.minusMonths(5) : fromMonth;
		String resolvedBucket = "WEEK".equalsIgnoreCase(bucket) ? "WEEK" : "MONTH";
		LocalDateTime fromDateTime = resolvedFromMonth.atDay(1).atStartOfDay();
		LocalDateTime toDateTime = resolvedToMonth.atEndOfMonth().atTime(23, 59, 59);
		List<Projection> projections = loadProjections(userId, fromDateTime, toDateTime);
		List<ExpenseSummaryDto.Point> points = "WEEK".equals(resolvedBucket)
			? buildWeeklyPoints(projections, fromDateTime.toLocalDate(), toDateTime.toLocalDate())
			: buildMonthlyPoints(projections, resolvedFromMonth, resolvedToMonth);
		ExpenseSummaryDto.Point latestPoint = points.isEmpty() ? null : points.get(points.size() - 1);
		ExpenseSummaryDto.Point previousPoint = points.size() > 1 ? points.get(points.size() - 2) : null;
		long latestAmount = latestPoint != null ? latestPoint.expenseAmount() : 0L;
		long previousAmount = previousPoint != null ? previousPoint.expenseAmount() : 0L;
		long changeAmount = latestAmount - previousAmount;
		double changeRate = previousAmount == 0L ? 0d : (changeAmount * 100d) / previousAmount;

		return new ExpenseSummaryDto.Response(
			new ExpenseSummaryDto.Range(resolvedFromMonth.toString(), resolvedToMonth.toString(), resolvedBucket),
			points,
			new ExpenseSummaryDto.Comparison(latestAmount, previousAmount, changeAmount, round(changeRate)),
			findLastAnalyzedAt(projections)
		);
	}

	public MonthlyExpenseDto.Response getMonthlyExpense(Integer userId, YearMonth month) {
		YearMonth targetMonth = month == null ? YearMonth.now() : month;
		LocalDateTime fromDateTime = targetMonth.atDay(1).atStartOfDay();
		LocalDateTime toDateTime = targetMonth.atEndOfMonth().atTime(23, 59, 59);
		List<Projection> projections = loadProjections(userId, fromDateTime, toDateTime);
		long fixedExpenseAmount = projections.stream()
			.filter(this::isPureExpense)
			.filter(projection -> resolveExpenseScope(projection) == ExpenseScope.FIXED)
			.mapToLong(projection -> projection.transaction().getTransactionBalance())
			.sum();
		long variableExpenseAmount = projections.stream()
			.filter(this::isPureExpense)
			.filter(projection -> resolveExpenseScope(projection) == ExpenseScope.VARIABLE)
			.mapToLong(projection -> projection.transaction().getTransactionBalance())
			.sum();
		long excludedIncomeAmount = projections.stream()
			.filter(projection -> hasExcludedReason(projection, TransactionExcludedReason.INCOME))
			.mapToLong(projection -> projection.transaction().getTransactionBalance())
			.sum();
		long excludedInternalTransferAmount = projections.stream()
			.filter(projection -> hasExcludedReason(projection, TransactionExcludedReason.INTERNAL_TRANSFER))
			.mapToLong(projection -> projection.transaction().getTransactionBalance())
			.sum();
		int transactionCount = (int) projections.stream().filter(this::isPureExpense).count();

		return new MonthlyExpenseDto.Response(
			targetMonth.toString(),
			fixedExpenseAmount + variableExpenseAmount,
			fixedExpenseAmount,
			variableExpenseAmount,
			excludedIncomeAmount,
			excludedInternalTransferAmount,
			transactionCount,
			findLastAnalyzedAt(projections)
		);
	}

	public TransactionAnalysisJobDto.OnboardingResult getOnboardingAnalysisResult(Integer userId, LocalDateTime referenceDateTime) {
		LocalDateTime resolvedReferenceDateTime = referenceDateTime == null ? LocalDateTime.now() : referenceDateTime;
		LocalDateTime fromDateTime = resolvedReferenceDateTime.minusMonths(3);
		List<Projection> projections = loadProjections(userId, fromDateTime, resolvedReferenceDateTime);

		long totalIncomeAmount = projections.stream()
			.filter(this::isIncome)
			.mapToLong(projection -> projection.transaction().getTransactionBalance())
			.sum();
		long totalExpenseAmount = projections.stream()
			.filter(this::isPureExpense)
			.mapToLong(projection -> projection.transaction().getTransactionBalance())
			.sum();
		long totalFixedExpenseAmount = projections.stream()
			.filter(this::isPureExpense)
			.filter(projection -> resolveExpenseScope(projection) == ExpenseScope.FIXED)
			.mapToLong(projection -> projection.transaction().getTransactionBalance())
			.sum();
		long totalVariableExpenseAmount = projections.stream()
			.filter(this::isPureExpense)
			.filter(projection -> resolveExpenseScope(projection) == ExpenseScope.VARIABLE)
			.mapToLong(projection -> projection.transaction().getTransactionBalance())
			.sum();

		long averageIncome = Math.round(totalIncomeAmount / 3d);
		long averageExpense = Math.round(totalExpenseAmount / 3d);
		long remainingAmount = averageIncome - averageExpense;
		double fixedExpenseRatio = totalExpenseAmount == 0L ? 0d : round((totalFixedExpenseAmount * 100d) / totalExpenseAmount);
		double variableExpenseRatio = totalExpenseAmount == 0L ? 0d : round((totalVariableExpenseAmount * 100d) / totalExpenseAmount);

		List<TransactionAnalysisJobDto.CategoryExpense> categoryExpenses = projections.stream()
			.filter(this::isPureExpense)
			.collect(Collectors.groupingBy(
				projection -> resolveCategory(projection),
				Collectors.summingLong(projection -> projection.transaction().getTransactionBalance())
			))
			.entrySet().stream()
			.sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
			.map(entry -> new TransactionAnalysisJobDto.CategoryExpense(
				entry.getKey().getId(),
				entry.getKey().getName(),
				entry.getValue()
			))
			.toList();

		return new TransactionAnalysisJobDto.OnboardingResult(
			"3개월 간 소비패턴을 분석했어요.",
			averageIncome,
			averageExpense,
			remainingAmount,
			fixedExpenseRatio,
			variableExpenseRatio,
			buildOnboardingInsightMessage(averageIncome, averageExpense),
			categoryExpenses
		);
	}

	public TransactionListDto.Response listTransactions(Integer userId, TransactionListDto.Query query) {
		LocalDate resolvedToDate = query.toDate() == null ? LocalDate.now() : query.toDate();
		LocalDate resolvedFromDate = query.fromDate() == null ? resolvedToDate.minusMonths(1) : query.fromDate();
		int requestedMonths = Math.max(1, (resolvedToDate.getYear() - resolvedFromDate.getYear()) * 12
			+ resolvedToDate.getMonthValue() - resolvedFromDate.getMonthValue() + 1);
		TransactionSyncService.SyncResult syncResult = syncLatestTransactionsUseCase.execute(userId, requestedMonths, false);

		List<Projection> filteredProjections = loadProjections(
			userId,
			resolvedFromDate.atStartOfDay(),
			resolvedToDate.atTime(23, 59, 59)
		).stream()
			.filter(projection -> matchesAnalysisInclusion(projection, query.analysisInclusion()))
			.toList();

		int page = query.page() == null ? 0 : query.page();
		int size = query.size() == null ? 20 : query.size();
		int fromIndex = Math.min(page * size, filteredProjections.size());
		int toIndex = Math.min(fromIndex + size, filteredProjections.size());
		List<TransactionListDto.Item> items = filteredProjections.subList(fromIndex, toIndex).stream()
			.map(this::toTransactionListItem)
			.toList();

		return new TransactionListDto.Response(
			new TransactionListDto.Sync(
				"SUCCEEDED",
				syncResult.insertedAccountCount() > 0 || syncResult.insertedCardCount() > 0,
				LocalDateTime.now()
			),
			page,
			size,
			(long) filteredProjections.size(),
			items
		);
	}

	public WeeklyTransactionAnalysisDto.Response getWeeklyTransactionAnalysis(Integer userId, String yearWeek) {
		EssentialExpenseSnapshotWeek snapshotWeek = essentialExpenseSnapshotWeekRepository.findByUser_IdAndYearWeek(userId, yearWeek)
			.orElseThrow(() -> new IllegalArgumentException("Weekly analysis not found"));
		List<Projection> projections = loadProjections(
			userId,
			snapshotWeek.getWeekStartDate().atStartOfDay(),
			snapshotWeek.getWeekEndDate().atTime(23, 59, 59)
		);
		List<WeeklyTransactionAnalysisDto.CategoryExpense> categoryExpenses = essentialExpenseSnapshotWeekCategoryRepository.findAll().stream()
			.filter(item -> item.getSnapshotWeek().getId().equals(snapshotWeek.getId()))
			.map(item -> new WeeklyTransactionAnalysisDto.CategoryExpense(
				item.getCategory().getId(),
				item.getCategory().getName(),
				item.getAmount()
			))
			.toList();

		return new WeeklyTransactionAnalysisDto.Response(
			snapshotWeek.getYearWeek(),
			snapshotWeek.getWeekStartDate(),
			snapshotWeek.getWeekEndDate(),
			projections.stream()
				.filter(this::isPureExpense)
				.mapToLong(projection -> projection.transaction().getTransactionBalance())
				.sum(),
			snapshotWeek.getEssentialAmount(),
			snapshotWeek.getFixedEssentialAmount(),
			snapshotWeek.getVariableEssentialAmount(),
			categoryExpenses,
			snapshotWeek.getAnalyzedAt()
		);
	}

	public MonthlyTransactionAnalysisDto.Response getMonthlyTransactionAnalysis(Integer userId, String yearMonth) {
		YearMonth targetMonth = YearMonth.parse(yearMonth);
		LocalDateTime fromDateTime = targetMonth.atDay(1).atStartOfDay();
		LocalDateTime toDateTime = targetMonth.atEndOfMonth().atTime(23, 59, 59);
		List<Projection> projections = loadProjections(userId, fromDateTime, toDateTime);
		List<EssentialExpenseSnapshotWeek> snapshots = essentialExpenseSnapshotWeekRepository
			.findAllByUser_IdAndWeekStartDateBetweenOrderByWeekStartDate(
				userId,
				targetMonth.atDay(1).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)),
				targetMonth.atEndOfMonth()
			);
		List<Integer> snapshotIds = snapshots.stream()
			.map(EssentialExpenseSnapshotWeek::getId)
			.toList();
		Map<Integer, Long> essentialCategoryAmounts = essentialExpenseSnapshotWeekCategoryRepository.findAll().stream()
			.filter(item -> snapshotIds.contains(item.getSnapshotWeek().getId()))
			.collect(Collectors.groupingBy(
				item -> item.getCategory().getId(),
				Collectors.summingLong(EssentialExpenseSnapshotWeekCategory::getAmount)
			));
		Map<Integer, Category> categoriesById = essentialExpenseSnapshotWeekCategoryRepository.findAll().stream()
			.filter(item -> snapshotIds.contains(item.getSnapshotWeek().getId()))
			.collect(Collectors.toMap(
				item -> item.getCategory().getId(),
				EssentialExpenseSnapshotWeekCategory::getCategory,
				(left, right) -> left
			));
		long expenseAmount = projections.stream()
			.filter(this::isPureExpense)
			.mapToLong(projection -> projection.transaction().getTransactionBalance())
			.sum();
		long monthlyEssentialAmount = snapshots.stream().mapToLong(EssentialExpenseSnapshotWeek::getEssentialAmount).sum();

		List<MonthlyTransactionAnalysisDto.CategoryExpense> categoryExpenses = essentialCategoryAmounts.entrySet().stream()
			.map(entry -> {
				Category category = categoriesById.get(entry.getKey());
				double ratio = expenseAmount == 0L ? 0d : (entry.getValue() * 100d) / expenseAmount;
				return new MonthlyTransactionAnalysisDto.CategoryExpense(
					category.getId(),
					category.getName(),
					entry.getValue(),
					round(ratio)
				);
			})
			.toList();

		return new MonthlyTransactionAnalysisDto.Response(
			targetMonth.toString(),
			expenseAmount,
			monthlyEssentialAmount,
			snapshots.stream().mapToLong(EssentialExpenseSnapshotWeek::getFixedEssentialAmount).sum(),
			snapshots.stream().mapToLong(EssentialExpenseSnapshotWeek::getVariableEssentialAmount).sum(),
			categoryExpenses,
			snapshots.stream().map(EssentialExpenseSnapshotWeek::getAnalyzedAt).max(LocalDateTime::compareTo).orElse(null)
		);
	}

	private List<Projection> loadProjections(Integer userId, LocalDateTime fromDateTime, LocalDateTime toDateTime) {
		List<TransactionOverride> transactions = new ArrayList<>();
		transactions.addAll(accountTransactionOverrideRepository
			.findAllByAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(userId, fromDateTime, toDateTime));
		transactions.addAll(cardTransactionOverrideRepository
			.findAllByCard_WithdrawalAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(
				userId,
				fromDateTime,
				toDateTime
			));
		transactions.sort(Comparator.comparing(TransactionOverride::getTransactionDateTime).reversed());
		Map<Integer, TransactionAnalysisResult> resultsByTransactionId = transactionAnalysisResultRepository.findAllByTransaction_IdIn(
			transactions.stream().map(TransactionOverride::getId).toList()
		).stream().collect(Collectors.toMap(result -> result.getTransaction().getId(), Function.identity()));

		return transactions.stream()
			.map(transaction -> new Projection(transaction, resultsByTransactionId.get(transaction.getId())))
			.toList();
	}

	private List<ExpenseSummaryDto.Point> buildMonthlyPoints(
		List<Projection> projections,
		YearMonth fromMonth,
		YearMonth toMonth
	) {
		List<ExpenseSummaryDto.Point> points = new ArrayList<>();
		YearMonth cursor = fromMonth;
		while (!cursor.isAfter(toMonth)) {
			final YearMonth targetMonth = cursor;
			points.add(toPoint(
				targetMonth.toString(),
				projections.stream()
					.filter(projection -> YearMonth.from(projection.transaction().getTransactionDateTime()).equals(targetMonth))
					.toList()
			));
			cursor = cursor.plusMonths(1);
		}
		return points;
	}

	private List<ExpenseSummaryDto.Point> buildWeeklyPoints(
		List<Projection> projections,
		LocalDate fromDate,
		LocalDate toDate
	) {
		List<ExpenseSummaryDto.Point> points = new ArrayList<>();
		LocalDate cursor = fromDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
		LocalDate end = toDate.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
		while (!cursor.isAfter(end)) {
			LocalDate weekStart = cursor;
			LocalDate weekEnd = cursor.plusDays(6);
			points.add(toPoint(
				weekStart + "~" + weekEnd,
				projections.stream()
					.filter(projection -> {
						LocalDate occurredDate = projection.transaction().getTransactionDateTime().toLocalDate();
						return !occurredDate.isBefore(weekStart) && !occurredDate.isAfter(weekEnd);
					})
					.toList()
			));
			cursor = cursor.plusWeeks(1);
		}
		return points;
	}

	private ExpenseSummaryDto.Point toPoint(String label, List<Projection> projections) {
		long fixedAmount = projections.stream()
			.filter(this::isPureExpense)
			.filter(projection -> resolveExpenseScope(projection) == ExpenseScope.FIXED)
			.mapToLong(projection -> projection.transaction().getTransactionBalance())
			.sum();
		long variableAmount = projections.stream()
			.filter(this::isPureExpense)
			.filter(projection -> resolveExpenseScope(projection) == ExpenseScope.VARIABLE)
			.mapToLong(projection -> projection.transaction().getTransactionBalance())
			.sum();
		return new ExpenseSummaryDto.Point(label, fixedAmount + variableAmount, fixedAmount, variableAmount);
	}

	private boolean isPureExpense(Projection projection) {
		if (hasExcludedReason(projection, TransactionExcludedReason.INCOME)
			|| hasExcludedReason(projection, TransactionExcludedReason.INTERNAL_TRANSFER)) {
			return false;
		}
		if (projection.transaction() instanceof AccountTransactionOverride accountTransaction) {
			return accountTransaction.getTransactionType() != 1;
		}
		return true;
	}

	private boolean isIncome(Projection projection) {
		if (projection.transaction() instanceof AccountTransactionOverride accountTransaction) {
			return accountTransaction.getTransactionType() == 1;
		}
		return hasExcludedReason(projection, TransactionExcludedReason.INCOME);
	}

	private boolean hasExcludedReason(Projection projection, TransactionExcludedReason excludedReason) {
		return projection.analysisResult() != null && projection.analysisResult().getExcludedReason() == excludedReason;
	}

	private ExpenseScope resolveExpenseScope(Projection projection) {
		if (projection.analysisResult() != null && projection.analysisResult().getExpenseScope() != null) {
			return projection.analysisResult().getExpenseScope();
		}
		return ExpenseScope.VARIABLE;
	}

	private LocalDateTime findLastAnalyzedAt(List<Projection> projections) {
		return projections.stream()
			.map(Projection::analysisResult)
			.filter(Objects::nonNull)
			.map(TransactionAnalysisResult::getAnalyzedAt)
			.max(LocalDateTime::compareTo)
			.orElse(null);
	}

	private boolean matchesAnalysisInclusion(Projection projection, String analysisInclusion) {
		if (analysisInclusion == null || analysisInclusion.isBlank() || "ALL".equalsIgnoreCase(analysisInclusion)) {
			return true;
		}
		boolean analysisIncluded = projection.analysisResult() != null && projection.analysisResult().isAnalysisIncluded();
		return "INCLUDED".equalsIgnoreCase(analysisInclusion) ? analysisIncluded : !analysisIncluded;
	}

	private Category resolveCategory(Projection projection) {
		return projection.analysisResult() != null ? projection.analysisResult().getCategory() : projection.transaction().getCategory();
	}

	private TransactionListDto.Item toTransactionListItem(Projection projection) {
		TransactionOverride transaction = projection.transaction();
		TransactionAnalysisResult analysisResult = projection.analysisResult();
		Category category = analysisResult != null ? analysisResult.getCategory() : transaction.getCategory();
		return new TransactionListDto.Item(
			transaction.getId(),
			transaction.getTransactionDateTime(),
			merchantTextOf(transaction),
			transaction.getTransactionBalance(),
			directionOf(transaction),
			new TransactionListDto.CategoryView(category.getId(), category.getName()),
			analysisResult != null && analysisResult.getExpenseScope() != null ? analysisResult.getExpenseScope().name() : null,
			analysisResult != null && analysisResult.isAnalysisIncluded(),
			analysisResult != null && analysisResult.getExcludedReason() != null ? analysisResult.getExcludedReason().name() : null
		);
	}

	private String merchantTextOf(TransactionOverride transaction) {
		if (transaction instanceof CardTransactionOverride cardTransaction) {
			return cardTransaction.getMerchant().getName();
		}
		AccountTransactionOverride accountTransaction = (AccountTransactionOverride) transaction;
		return accountTransaction.getTransactionSummary();
	}

	private String directionOf(TransactionOverride transaction) {
		if (transaction instanceof AccountTransactionOverride accountTransaction) {
			return accountTransaction.getTransactionType() == 1 ? "IN" : "OUT";
		}
		return "OUT";
	}

	private String buildOnboardingInsightMessage(long averageIncome, long averageExpense) {
		if (averageIncome <= 0L) {
			return "소비 분석 데이터가 아직 충분하지 않아요.";
		}
		if (averageExpense >= Math.round(averageIncome * 0.6d)) {
			return "소비 절약이 필요해보여요.";
		}
		return "현재 소비 흐름을 잘 유지하고 있어요.";
	}

	private double round(double value) {
		return Math.round(value * 100d) / 100d;
	}

	private record Projection(
		TransactionOverride transaction,
		TransactionAnalysisResult analysisResult
	) {
	}
}

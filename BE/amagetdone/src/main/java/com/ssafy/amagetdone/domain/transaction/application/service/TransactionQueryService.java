package com.ssafy.amagetdone.domain.transaction.application.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeekCategory;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekCategoryRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseGraphDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionListDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyExpenseGraphDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.application.usecase.SyncLatestTransactionsUseCase;
import com.ssafy.amagetdone.domain.transaction.exception.TransactionErrorCode;
import com.ssafy.amagetdone.domain.transaction.exception.TransactionException;
import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisMetadata;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisResult;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.repository.AccountTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.CardTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisMetadataRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisResultRepository;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionExcludedReason;
import com.ssafy.amagetdone.domain.transaction.support.util.TransactionMerchantTextResolver;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TransactionQueryService {

	private static final String YEAR_WEEK_FORMAT_MESSAGE = "yearWeek must follow yyyy-MM-w.";

	private final AccountTransactionOverrideRepository accountTransactionOverrideRepository;
	private final CardTransactionOverrideRepository cardTransactionOverrideRepository;
	private final TransactionAnalysisResultRepository transactionAnalysisResultRepository;
	private final TransactionAnalysisMetadataRepository transactionAnalysisMetadataRepository;
	private final EssentialExpenseSnapshotWeekRepository essentialExpenseSnapshotWeekRepository;
	private final EssentialExpenseSnapshotWeekCategoryRepository essentialExpenseSnapshotWeekCategoryRepository;
	private final SyncLatestTransactionsUseCase syncLatestTransactionsUseCase;

	public MonthlyExpenseDto.Response getMonthlyExpense(Integer userId, YearMonth month) {
		YearMonth targetMonth = month == null ? YearMonth.now() : month;
		LocalDateTime fromDateTime = targetMonth.atDay(1).atStartOfDay();
		LocalDateTime toDateTime = targetMonth.atEndOfMonth().atTime(23, 59, 59);
		List<Projection> projections = loadProjections(userId, fromDateTime, toDateTime, null);
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

	public TransactionAnalysisDto.OnboardingResult getOnboardingAnalysisResult(Integer userId) {
		TransactionAnalysisMetadata latestMetadata = getLatestMetadata(userId);
		List<Projection> projections = loadProjections(
			userId,
			latestMetadata.getWindowStartAt(),
			latestMetadata.getWindowEndAt(),
			latestMetadata.getAnalyzedAt()
		);

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
		int requestedMonths = latestMetadata.getRequestedMonths() == null || latestMetadata.getRequestedMonths() <= 0
			? 1
			: latestMetadata.getRequestedMonths();

		long averageIncome = Math.round(totalIncomeAmount / (double) requestedMonths);
		long averageExpense = Math.round(totalExpenseAmount / (double) requestedMonths);
		long remainingAmount = averageIncome - averageExpense;
		double fixedExpenseRatio = totalExpenseAmount == 0L ? 0d : round((totalFixedExpenseAmount * 100d) / totalExpenseAmount);
		double variableExpenseRatio = totalExpenseAmount == 0L ? 0d : round((totalVariableExpenseAmount * 100d) / totalExpenseAmount);

		List<TransactionAnalysisDto.CategoryExpense> categoryExpenses = projections.stream()
			.filter(this::isPureExpense)
			.collect(Collectors.groupingBy(
				this::resolveCategory,
				Collectors.summingLong(projection -> projection.transaction().getTransactionBalance())
			))
			.entrySet().stream()
			.sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
			.map(entry -> new TransactionAnalysisDto.CategoryExpense(
				entry.getKey().getId(),
				entry.getKey().getName(),
				Math.round(entry.getValue() / (double) requestedMonths)
			))
			.toList();

		return new TransactionAnalysisDto.OnboardingResult(
			"최근 " + latestMetadata.getRequestedMonths() + "개월 소비 분석을 완료했어요.",
			averageIncome,
			averageExpense,
			remainingAmount,
			fixedExpenseRatio,
			variableExpenseRatio,
			buildOnboardingInsightMessage(averageIncome, averageExpense),
			categoryExpenses
		);
	}

	public TransactionAnalysisDto.OnboardingResult getMonthlyExecutionAnalysisResult(
		Integer userId,
		TransactionAnalysisMetadata metadata,
		YearMonth month
	) {
		List<Projection> projections = loadProjections(
			userId,
			metadata.getWindowStartAt(),
			metadata.getWindowEndAt(),
			metadata.getAnalyzedAt()
		);

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
		int requestedMonths = metadata.getRequestedMonths() == null || metadata.getRequestedMonths() <= 0
			? 1
			: metadata.getRequestedMonths();

		long averageIncome = Math.round(totalIncomeAmount / (double) requestedMonths);
		long averageExpense = Math.round(totalExpenseAmount / (double) requestedMonths);
		long remainingAmount = averageIncome - averageExpense;
		double fixedExpenseRatio = totalExpenseAmount == 0L ? 0d : round((totalFixedExpenseAmount * 100d) / totalExpenseAmount);
		double variableExpenseRatio = totalExpenseAmount == 0L ? 0d : round((totalVariableExpenseAmount * 100d) / totalExpenseAmount);

		List<TransactionAnalysisDto.CategoryExpense> categoryExpenses = projections.stream()
			.filter(this::isPureExpense)
			.collect(Collectors.groupingBy(
				this::resolveCategory,
				Collectors.summingLong(projection -> projection.transaction().getTransactionBalance())
			))
			.entrySet().stream()
			.sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
			.map(entry -> new TransactionAnalysisDto.CategoryExpense(
				entry.getKey().getId(),
				entry.getKey().getName(),
				Math.round(entry.getValue() / (double) requestedMonths)
			))
			.toList();

		return new TransactionAnalysisDto.OnboardingResult(
			month + " 소비 분석을 완료했어요.",
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
			resolvedToDate.atTime(23, 59, 59),
			null
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

	public List<TransactionListDto.Item> listExpenseTransactions(
		Integer userId,
		LocalDate fromDate,
		LocalDate toDate
	) {
		return loadProjections(userId, fromDate.atStartOfDay(), toDate.atTime(23, 59, 59), null).stream()
			.filter(this::isPureExpense)
			.map(this::toTransactionListItem)
			.toList();
	}

	public List<TransactionListDto.Item> listNonEssentialFreeSpendingTransactions(
		Integer userId,
		LocalDate fromDate,
		LocalDate toDate
	) {
		return loadProjections(userId, fromDate.atStartOfDay(), toDate.atTime(23, 59, 59), null).stream()
			.filter(this::isNonEssentialFreeSpending)
			.map(this::toTransactionListItem)
			.toList();
	}

	public WeeklyTransactionAnalysisDto.Response getWeeklyTransactionAnalysis(Integer userId, String yearWeek) {
		ParsedYearWeek parsedYearWeek = parseYearMonthWeek(yearWeek);
		LocalDate weekStartDate = parsedYearWeek.weekStartDate();
		LocalDate weekEndDate = weekStartDate.plusDays(6L);
		LocalDateTime analyzedAt = transactionAnalysisMetadataRepository.findTopByUser_IdOrderByAnalyzedAtDesc(userId)
			.map(TransactionAnalysisMetadata::getAnalyzedAt)
			.orElse(null);
		List<Projection> projections = loadProjections(
			userId,
			weekStartDate.atStartOfDay(),
			weekEndDate.atTime(23, 59, 59),
			analyzedAt
		);
		List<Projection> pureExpenseProjections = projections.stream()
			.filter(this::isPureExpense)
			.toList();
		if (pureExpenseProjections.isEmpty()) {
			return WeeklyTransactionAnalysisDto.Response.empty(parsedYearWeek.responseYearWeek());
		}
		List<Projection> essentialExpenseProjections = pureExpenseProjections.stream()
			.filter(this::isEssentialExpense)
			.toList();
		List<WeeklyTransactionAnalysisDto.CategoryExpense> categoryExpenses = buildWeeklyCategoryExpenses(essentialExpenseProjections);
		long expenseAmount = pureExpenseProjections.stream()
			.mapToLong(projection -> projection.transaction().getTransactionBalance())
			.sum();
		long essentialAmount = essentialExpenseProjections.stream()
			.mapToLong(projection -> projection.transaction().getTransactionBalance())
			.sum();

		return new WeeklyTransactionAnalysisDto.Response(
			parsedYearWeek.responseYearWeek(),
			weekStartDate,
			weekEndDate,
			expenseAmount,
			essentialAmount,
			sumExpenseAmountByScope(essentialExpenseProjections, ExpenseScope.FIXED),
			sumExpenseAmountByScope(essentialExpenseProjections, ExpenseScope.VARIABLE),
			categoryExpenses,
			findLastAnalyzedAt(projections)
		);
	}

	public WeeklyExpenseGraphDto.Response getWeeklyExpenseGraph(Integer userId, String yearWeek) {
		ParsedYearWeek parsedYearWeek = parseYearMonthWeek(yearWeek);
		LocalDate weekStartDate = parsedYearWeek.weekStartDate();
		LocalDate weekEndDate = weekStartDate.plusDays(6L);
		List<Projection> loadedProjections = loadProjections(
			userId,
			weekStartDate.atStartOfDay(),
			weekEndDate.atTime(23, 59, 59),
			null
		);
		List<Projection> projections = filterByTransactionDateRange(loadedProjections, weekStartDate, weekEndDate);
		List<Projection> pureExpenseProjections = projections.stream()
			.filter(this::isPureExpense)
			.toList();

		return new WeeklyExpenseGraphDto.Response(
			parsedYearWeek.responseYearWeek(),
			weekStartDate,
			weekEndDate,
			pureExpenseProjections.stream()
				.mapToLong(projection -> projection.transaction().getTransactionBalance())
				.sum(),
			buildDailyExpensePoints(weekStartDate, pureExpenseProjections),
			findLastAnalyzedAt(projections)
		);
	}

	public MonthlyExpenseGraphDto.Response getMonthlyExpenseGraph(Integer userId, String yearMonth) {
		YearMonth targetMonth = YearMonth.parse(yearMonth);
		LocalDate monthStartDate = targetMonth.atDay(1);
		LocalDate monthEndDate = targetMonth.atEndOfMonth();
		List<Projection> loadedProjections = loadProjections(
			userId,
			monthStartDate.atStartOfDay(),
			monthEndDate.atTime(23, 59, 59),
			null
		);
		List<Projection> projections = filterByTransactionDateRange(loadedProjections, monthStartDate, monthEndDate);
		List<Projection> pureExpenseProjections = projections.stream()
			.filter(this::isPureExpense)
			.toList();

		return new MonthlyExpenseGraphDto.Response(
			targetMonth.toString(),
			monthStartDate,
			monthEndDate,
			pureExpenseProjections.stream()
				.mapToLong(projection -> projection.transaction().getTransactionBalance())
				.sum(),
			buildMonthlyExpensePoints(targetMonth, pureExpenseProjections),
			findLastAnalyzedAt(projections)
		);
	}

	public MonthlyTransactionAnalysisDto.Response getMonthlyTransactionAnalysis(Integer userId, String yearMonth) {
		TransactionAnalysisMetadata latestMetadata = getLatestMetadata(userId);
		YearMonth targetMonth = YearMonth.parse(yearMonth);
		LocalDateTime fromDateTime = targetMonth.atDay(1).atStartOfDay();
		LocalDateTime toDateTime = targetMonth.atEndOfMonth().atTime(23, 59, 59);
		List<Projection> projections = loadProjections(userId, fromDateTime, toDateTime, latestMetadata.getAnalyzedAt());
		List<EssentialExpenseSnapshotWeek> snapshots = essentialExpenseSnapshotWeekRepository
			.findAllByUser_IdAndAnalyzedAtAndWeekStartDateBetweenOrderByWeekStartDate(
				userId,
				latestMetadata.getAnalyzedAt(),
				targetMonth.atDay(1).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)),
				targetMonth.atEndOfMonth()
			);
		List<Integer> snapshotIds = snapshots.stream()
			.map(EssentialExpenseSnapshotWeek::getId)
			.toList();
		List<EssentialExpenseSnapshotWeekCategory> snapshotCategories = snapshotIds.isEmpty()
			? List.of()
			: essentialExpenseSnapshotWeekCategoryRepository.findAllBySnapshotWeek_IdIn(snapshotIds);
		Map<Integer, Long> essentialCategoryAmounts = snapshotCategories.stream()
			.collect(Collectors.groupingBy(
				item -> item.getCategory().getId(),
				Collectors.summingLong(EssentialExpenseSnapshotWeekCategory::getAmount)
			));
		Map<Integer, Category> categoriesById = snapshotCategories.stream()
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

	private TransactionAnalysisMetadata getLatestMetadata(Integer userId) {
		return transactionAnalysisMetadataRepository.findTopByUser_IdOrderByAnalyzedAtDesc(userId)
			.orElseThrow(() -> new TransactionException(TransactionErrorCode.TRANSACTION_ANALYSIS_RESULT_NOT_FOUND));
	}

	private ParsedYearWeek parseYearMonthWeek(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(YEAR_WEEK_FORMAT_MESSAGE);
		}
		String[] tokens = value.split("-");
		if (tokens.length != 3) {
			throw new IllegalArgumentException(YEAR_WEEK_FORMAT_MESSAGE);
		}
		try {
			YearMonth targetMonth = YearMonth.of(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]));
			int weekOfMonth = Integer.parseInt(tokens[2]);
			LocalDate weekStartDate = resolveWeekStartDate(targetMonth, weekOfMonth);
			return new ParsedYearWeek(toExternalYearMonthWeek(weekStartDate), weekStartDate);
		} catch (RuntimeException exception) {
			throw new IllegalArgumentException(YEAR_WEEK_FORMAT_MESSAGE, exception);
		}
	}

	private LocalDate resolveWeekStartDate(YearMonth targetMonth, int weekOfMonth) {
		if (weekOfMonth <= 0) {
			throw new IllegalArgumentException(YEAR_WEEK_FORMAT_MESSAGE);
		}
		LocalDate firstWeekStartDate = targetMonth.atDay(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
		LocalDate weekStartDate = firstWeekStartDate.plusWeeks(weekOfMonth - 1L);
		if (!YearMonth.from(weekStartDate).equals(targetMonth)) {
			throw new IllegalArgumentException(YEAR_WEEK_FORMAT_MESSAGE);
		}
		return weekStartDate;
	}

	private String toExternalYearMonthWeek(LocalDate weekStartDate) {
		YearMonth targetMonth = YearMonth.from(weekStartDate);
		LocalDate firstWeekStartDate = targetMonth.atDay(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
		long weekIndex = ChronoUnit.WEEKS.between(firstWeekStartDate, weekStartDate) + 1L;
		if (weekIndex <= 0) {
			throw new IllegalArgumentException(YEAR_WEEK_FORMAT_MESSAGE);
		}
		return targetMonth + "-" + weekIndex;
	}

	private List<Projection> loadProjections(
		Integer userId,
		LocalDateTime fromDateTime,
		LocalDateTime toDateTime,
		LocalDateTime analyzedAt
	) {
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
		List<Integer> transactionIds = transactions.stream().map(TransactionOverride::getId).toList();
		List<TransactionAnalysisResult> analysisResults = transactionIds.isEmpty()
			? List.of()
			: analyzedAt == null
				? transactionAnalysisResultRepository.findAllByTransaction_IdIn(transactionIds)
				: transactionAnalysisResultRepository.findAllByTransaction_IdInAndAnalyzedAt(transactionIds, analyzedAt);
		Map<Integer, TransactionAnalysisResult> resultsByTransactionId = analysisResults.stream()
			.collect(Collectors.toMap(
				result -> result.getTransaction().getId(),
				Function.identity(),
				(left, right) -> left.getAnalyzedAt().isAfter(right.getAnalyzedAt()) ? left : right
			));

		return transactions.stream()
			.map(transaction -> new Projection(transaction, resultsByTransactionId.get(transaction.getId())))
			.toList();
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

	private boolean isNonEssentialFreeSpending(Projection projection) {
		TransactionAnalysisResult analysisResult = projection.analysisResult();
		if (analysisResult == null || analysisResult.isAnalysisIncluded()) {
			return false;
		}

		TransactionExcludedReason excludedReason = analysisResult.getExcludedReason();
		if (excludedReason == null) {
			return false;
		}

		return excludedReason == TransactionExcludedReason.NON_ESSENTIAL;
	}

	private boolean isEssentialExpense(Projection projection) {
		TransactionAnalysisResult analysisResult = projection.analysisResult();
		return analysisResult != null && analysisResult.isAnalysisIncluded();
	}

	private List<WeeklyExpenseGraphDto.Point> buildDailyExpensePoints(LocalDate weekStartDate, List<Projection> projections) {
		Map<LocalDate, Long> expenseAmountByDate = projections.stream()
			.collect(Collectors.groupingBy(
				projection -> projection.transaction().getTransactionDateTime().toLocalDate(),
				Collectors.summingLong(projection -> projection.transaction().getTransactionBalance())
			));
		List<WeeklyExpenseGraphDto.Point> points = new ArrayList<>();
		for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
			LocalDate targetDate = weekStartDate.plusDays(dayOffset);
			points.add(new WeeklyExpenseGraphDto.Point(
				targetDate,
				dayOfWeekLabel(targetDate.getDayOfWeek()),
				expenseAmountByDate.getOrDefault(targetDate, 0L)
			));
		}
		return points;
	}

	private List<MonthlyExpenseGraphDto.Point> buildMonthlyExpensePoints(YearMonth targetMonth, List<Projection> projections) {
		LocalDate monthStartDate = targetMonth.atDay(1);
		LocalDate monthEndDate = targetMonth.atEndOfMonth();
		LocalDate cursor = monthStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate lastWeekStart = monthEndDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		List<MonthlyExpenseGraphDto.Point> points = new ArrayList<>();
		int index = 1;
		while (!cursor.isAfter(lastWeekStart)) {
			LocalDate weekStartDate = cursor;
			LocalDate weekEndDate = cursor.plusDays(6L);
			long expenseAmount = projections.stream()
				.filter(projection -> {
					LocalDate transactionDate = projection.transaction().getTransactionDateTime().toLocalDate();
					return !transactionDate.isBefore(monthStartDate)
						&& !transactionDate.isAfter(monthEndDate)
						&& !transactionDate.isBefore(weekStartDate)
						&& !transactionDate.isAfter(weekEndDate);
				})
				.mapToLong(projection -> projection.transaction().getTransactionBalance())
				.sum();
			points.add(new MonthlyExpenseGraphDto.Point(
				toExternalYearMonthWeek(weekStartDate),
				weekStartDate,
				weekEndDate,
				targetMonth.getMonthValue() + "월 " + index + "주차",
				expenseAmount
			));
			cursor = cursor.plusWeeks(1);
			index++;
		}
		return points;
	}

	private ExpenseScope resolveExpenseScope(Projection projection) {
		if (projection.analysisResult() != null && projection.analysisResult().getExpenseScope() != null) {
			return projection.analysisResult().getExpenseScope();
		}
		return ExpenseScope.VARIABLE;
	}

	private List<WeeklyTransactionAnalysisDto.CategoryExpense> buildWeeklyCategoryExpenses(List<Projection> projections) {
		Map<WeeklyCategoryKey, Long> amountsByCategory = projections.stream()
			.collect(Collectors.groupingBy(
				projection -> {
					Category category = resolveCategory(projection);
					return new WeeklyCategoryKey(category.getId(), category.getName());
				},
				LinkedHashMap::new,
				Collectors.summingLong(projection -> projection.transaction().getTransactionBalance())
			));
		return amountsByCategory.entrySet().stream()
			.sorted(Map.Entry.comparingByKey(Comparator.comparing(WeeklyCategoryKey::categoryId)))
			.map(entry -> new WeeklyTransactionAnalysisDto.CategoryExpense(
				entry.getKey().categoryId(),
				entry.getKey().categoryName(),
				entry.getValue(),
				entry.getValue(),
				0L
			))
			.toList();
	}

	private long sumExpenseAmountByScope(List<Projection> projections, ExpenseScope expenseScope) {
		return projections.stream()
			.filter(projection -> resolveExpenseScope(projection) == expenseScope)
			.mapToLong(projection -> projection.transaction().getTransactionBalance())
			.sum();
	}

	private long zero(Long amount) {
		return amount == null ? 0L : amount;
	}

	private List<Projection> filterByTransactionDateRange(
		List<Projection> projections,
		LocalDate startDate,
		LocalDate endDate
	) {
		return projections.stream()
			.filter(projection -> {
				LocalDate transactionDate = projection.transaction().getTransactionDateTime().toLocalDate();
				return !transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate);
			})
			.toList();
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
			sourceTypeOf(transaction),
			sourceIdOf(transaction),
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

	private String sourceTypeOf(TransactionOverride transaction) {
		return transaction instanceof CardTransactionOverride ? "CARD" : "ACCOUNT";
	}

	private String sourceIdOf(TransactionOverride transaction) {
		if (transaction instanceof CardTransactionOverride cardTransaction) {
			return String.valueOf(cardTransaction.getCard().getCardUniqueNo());
		}
		AccountTransactionOverride accountTransaction = (AccountTransactionOverride) transaction;
		return accountTransaction.getAccount().getId();
	}

	private String merchantTextOf(TransactionOverride transaction) {
		return TransactionMerchantTextResolver.resolve(transaction);
	}

	private String directionOf(TransactionOverride transaction) {
		if (transaction instanceof AccountTransactionOverride accountTransaction) {
			return accountTransaction.getTransactionType() == 1 ? "IN" : "OUT";
		}
		return "OUT";
	}

	private String buildOnboardingInsightMessage(long averageIncome, long averageExpense) {
		if (averageIncome <= 0L) {
			return "아직 분석할 수입 데이터가 충분하지 않아요.";
		}
		if (averageExpense >= Math.round(averageIncome * 0.6d)) {
			return "지출을 조금만 줄이면 더 여유가 생겨요.";
		}
		return "현재 소비 흐름은 안정적이에요.";
	}

	private double round(double value) {
		return Math.round(value * 100d) / 100d;
	}

	private String dayOfWeekLabel(DayOfWeek dayOfWeek) {
		return switch (dayOfWeek) {
			case MONDAY -> "월";
			case TUESDAY -> "화";
			case WEDNESDAY -> "수";
			case THURSDAY -> "목";
			case FRIDAY -> "금";
			case SATURDAY -> "토";
			case SUNDAY -> "일";
		};
	}

	private record Projection(
		TransactionOverride transaction,
		TransactionAnalysisResult analysisResult
	) {
	}

	private record WeeklyCategoryKey(
		Integer categoryId,
		String categoryName
	) {
	}

	private record ParsedYearWeek(
		String responseYearWeek,
		LocalDate weekStartDate
	) {
	}
}

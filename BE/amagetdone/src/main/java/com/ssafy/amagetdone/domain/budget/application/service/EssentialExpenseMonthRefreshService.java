package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseMonth;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetMonthRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseMonthRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetMonthStatus;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;
import com.ssafy.amagetdone.domain.budget.model.vo.SourceProvider;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisMetadata;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisResult;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisMetadataRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisResultRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional
public class EssentialExpenseMonthRefreshService {

	private static final int REFRESH_MONTHS = 3;
	private static final long ROUNDING_UNIT = 100L;

	private final TransactionAnalysisMetadataRepository transactionAnalysisMetadataRepository;
	private final TransactionAnalysisResultRepository transactionAnalysisResultRepository;
	private final BudgetMonthRepository budgetMonthRepository;
	private final EssentialExpenseMonthRepository essentialExpenseMonthRepository;

	public EssentialExpenseMonthRefreshService(
		TransactionAnalysisMetadataRepository transactionAnalysisMetadataRepository,
		TransactionAnalysisResultRepository transactionAnalysisResultRepository,
		BudgetMonthRepository budgetMonthRepository,
		EssentialExpenseMonthRepository essentialExpenseMonthRepository
	) {
		this.transactionAnalysisMetadataRepository = transactionAnalysisMetadataRepository;
		this.transactionAnalysisResultRepository = transactionAnalysisResultRepository;
		this.budgetMonthRepository = budgetMonthRepository;
		this.essentialExpenseMonthRepository = essentialExpenseMonthRepository;
	}

	public void refresh(Integer userId, LocalDate baseMonth) {
		Optional<TransactionAnalysisMetadata> latestMetadata = transactionAnalysisMetadataRepository
			.findTopByUser_IdOrderByAnalyzedAtDesc(userId);
		if (latestMetadata.isEmpty()) {
			return;
		}

		TransactionAnalysisMetadata metadata = latestMetadata.get();
		LocalDate normalizedBaseMonth = baseMonth.withDayOfMonth(1);
		LocalDateTime windowEndAt = metadata.getWindowEndAt();
		LocalDateTime windowStartAt = YearMonth.from(windowEndAt)
			.minusMonths(REFRESH_MONTHS - 1L)
			.atDay(1)
			.atStartOfDay();
		List<TransactionAnalysisResult> latestResults = transactionAnalysisResultRepository
			.findAllByUserIdAndAnalyzedAtAndTransactionDateTimeBetween(
				userId,
				metadata.getAnalyzedAt(),
				windowStartAt,
				windowEndAt
			);

		Set<YearMonth> validMonths = resolveValidMonths(latestResults);
		if (validMonths.isEmpty()) {
			return;
		}

		List<EssentialExpenseMonth> existingRows = essentialExpenseMonthRepository
			.findAllByBudgetMonth_User_IdAndBudgetMonth_BaseDate(userId, normalizedBaseMonth);
		Set<AggregateKey> userInputKeys = existingRows.stream()
			.filter(row -> row.getSourceProvider() == SourceProvider.USER_INPUT)
			.map(AggregateKey::from)
			.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
		Map<AggregateKey, AggregateAmount> desiredAmounts = aggregateDesiredAmounts(latestResults, validMonths.size(), userInputKeys);
		Map<AggregateKey, EssentialExpenseMonth> existingAnalyzerRows = existingRows.stream()
			.filter(row -> row.getSourceProvider() == SourceProvider.ANALYZER)
			.collect(java.util.stream.Collectors.toMap(
				AggregateKey::from,
				row -> row,
				(left, right) -> left,
				LinkedHashMap::new
			));

		List<EssentialExpenseMonth> rowsToDelete = existingAnalyzerRows.entrySet().stream()
			.filter(entry -> !desiredAmounts.containsKey(entry.getKey()))
			.map(Map.Entry::getValue)
			.toList();
		if (!rowsToDelete.isEmpty()) {
			essentialExpenseMonthRepository.deleteAll(rowsToDelete);
		}

		if (desiredAmounts.isEmpty()) {
			return;
		}

		BudgetMonth budgetMonth = budgetMonthRepository.findByUser_IdAndBaseDate(userId, normalizedBaseMonth)
			.orElseGet(() -> budgetMonthRepository.save(BudgetMonth.builder()
				.user(userReference(userId))
				.baseDate(normalizedBaseMonth)
				.status(BudgetMonthStatus.OPEN)
				.build()));

		List<EssentialExpenseMonth> rowsToSave = new ArrayList<>();
		for (Map.Entry<AggregateKey, AggregateAmount> entry : desiredAmounts.entrySet()) {
			AggregateKey key = entry.getKey();
			AggregateAmount desired = entry.getValue();
			EssentialExpenseMonth row = existingAnalyzerRows.get(key);
			if (row == null) {
				row = EssentialExpenseMonth.builder()
					.budgetMonth(budgetMonth)
					.category(desired.category())
					.expenseScope(key.expenseScope())
					.sourceProvider(SourceProvider.ANALYZER)
					.build();
			}
			row.setBudgetMonth(budgetMonth);
			row.setCategory(desired.category());
			row.setExpenseScope(key.expenseScope());
			row.setAmount(desired.amount());
			row.setSourceProvider(SourceProvider.ANALYZER);
			rowsToSave.add(row);
		}

		if (!CollectionUtils.isEmpty(rowsToSave)) {
			essentialExpenseMonthRepository.saveAll(rowsToSave);
		}
	}

	private Set<YearMonth> resolveValidMonths(List<TransactionAnalysisResult> latestResults) {
		return latestResults.stream()
			.map(TransactionAnalysisResult::getTransaction)
			.filter(java.util.Objects::nonNull)
			.map(transaction -> transaction.getTransactionDateTime())
			.filter(java.util.Objects::nonNull)
			.map(YearMonth::from)
			.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
	}

	private Map<AggregateKey, AggregateAmount> aggregateDesiredAmounts(
		List<TransactionAnalysisResult> latestResults,
		int validMonthCount,
		Set<AggregateKey> userInputKeys
	) {
		Map<AggregateKey, AggregateSeed> totalsByKey = new LinkedHashMap<>();
		for (TransactionAnalysisResult result : latestResults) {
			if (!result.isAnalysisIncluded() || result.getCategory() == null || result.getExpenseScope() == null) {
				continue;
			}

			AggregateKey key = new AggregateKey(result.getCategory().getId(), result.getExpenseScope().name());
			if (userInputKeys.contains(key)) {
				continue;
			}

			AggregateSeed seed = totalsByKey.computeIfAbsent(
				key,
				ignored -> new AggregateSeed(result.getCategory(), result.getExpenseScope(), 0L)
			);
			seed.add(result.getTransaction().getTransactionBalance());
		}

		Map<AggregateKey, AggregateAmount> desiredAmounts = new LinkedHashMap<>();
		for (Map.Entry<AggregateKey, AggregateSeed> entry : totalsByKey.entrySet()) {
			AggregateSeed seed = entry.getValue();
			long averagedAmount = roundToNearestHundred(seed.totalAmount() / (double)validMonthCount);
			desiredAmounts.put(entry.getKey(), new AggregateAmount(seed.category(), averagedAmount));
		}
		return desiredAmounts;
	}

	private long roundToNearestHundred(double amount) {
		return Math.round(amount / ROUNDING_UNIT) * ROUNDING_UNIT;
	}

	private User userReference(Integer userId) {
		User user = User.createPendingSocialUser();
		user.setId(userId);
		return user;
	}

	private record AggregateKey(
		Integer categoryId,
		String expenseScope
	) {
		private static AggregateKey from(EssentialExpenseMonth row) {
			return new AggregateKey(row.getCategory().getId(), row.getExpenseScope());
		}
	}

	private record AggregateAmount(
		Category category,
		long amount
	) {
	}

	private static final class AggregateSeed {

		private final Category category;
		private final ExpenseScope expenseScope;
		private long totalAmount;

		private AggregateSeed(Category category, ExpenseScope expenseScope, long totalAmount) {
			this.category = category;
			this.expenseScope = expenseScope;
			this.totalAmount = totalAmount;
		}

		private void add(long amount) {
			totalAmount += amount;
		}

		private Category category() {
			return category;
		}

		private long totalAmount() {
			return totalAmount;
		}

		@SuppressWarnings("unused")
		private ExpenseScope expenseScope() {
			return expenseScope;
		}
	}
}

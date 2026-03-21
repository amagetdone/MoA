package com.ssafy.amagetdone.domain.transaction.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeekCategory;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekCategoryRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
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
import com.ssafy.amagetdone.domain.transaction.support.util.FixedExpenseClassifier;
import com.ssafy.amagetdone.domain.transaction.support.util.TransactionExclusionPolicy;
import com.ssafy.amagetdone.domain.transaction.support.util.WeeklyEssentialAllocator;
import com.ssafy.amagetdone.domain.transaction.support.util.WeeklyEssentialAllocator.Entry;
import com.ssafy.amagetdone.domain.transaction.support.util.WeeklyEssentialAllocator.WeeklySnapshot;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionAnalysisService {

	private static final int DEFAULT_MONTHS = 6;
	private static final int WEEKLY_ESSENTIAL_BASE_MONTHS = 3;

	private final UserRepository userRepository;
	private final AccountRepository accountRepository;
	private final AccountTransactionOverrideRepository accountTransactionOverrideRepository;
	private final CardTransactionOverrideRepository cardTransactionOverrideRepository;
	private final TransactionAnalysisResultRepository transactionAnalysisResultRepository;
	private final TransactionAnalysisMetadataRepository transactionAnalysisMetadataRepository;
	private final EssentialExpenseSnapshotWeekRepository essentialExpenseSnapshotWeekRepository;
	private final EssentialExpenseSnapshotWeekCategoryRepository essentialExpenseSnapshotWeekCategoryRepository;
	private final TransactionExclusionPolicy transactionExclusionPolicy;
	private final FixedExpenseClassifier fixedExpenseClassifier;
	private final WeeklyEssentialAllocator weeklyEssentialAllocator;

	private Clock clock = Clock.systemDefaultZone();

	@Transactional
	public TransactionAnalysisMetadata analyzeTransactions(Integer userId, Integer months) {
		int requestedMonths = normalizeRequestedMonths(months);
		return analyzeTransactions(userId, requestedMonths, resolveCompletedMonthWindow(requestedMonths));
	}

	@Transactional
	public TransactionAnalysisMetadata analyzeTransactions(Integer userId, YearMonth month) {
		return analyzeTransactions(userId, 1, resolveMonthWindow(month));
	}

	private TransactionAnalysisMetadata analyzeTransactions(
		Integer userId,
		int requestedMonths,
		AnalysisWindow analysisWindow
	) {
		List<TransactionOverride> sourceTransactions = loadTransactions(
			userId,
			analysisWindow.windowStartAt(),
			analysisWindow.windowEndAt()
		);
		AnalysisSignature currentSignature = AnalysisSignature.from(sourceTransactions);

		User user = userRepository.getReferenceById(userId);
		LocalDateTime analyzedAt = LocalDateTime.now(clock);
		Set<String> registeredAccountNos = accountRepository.findAllByUser_Id(userId).stream()
			.map(Account::getAccountNo)
			.filter(this::hasText)
			.map(String::trim)
			.collect(java.util.stream.Collectors.toSet());

		List<PreparedTransaction> preparedTransactions = sourceTransactions.stream()
			.map(transaction -> prepareTransaction(transaction, registeredAccountNos))
			.toList();

		List<FixedExpenseClassifier.Candidate> classifierCandidates = new ArrayList<>();
		LinkedHashMap<Integer, Resolution> resolutionsByTransactionId = new LinkedHashMap<>();
		for (PreparedTransaction preparedTransaction : preparedTransactions) {
			TransactionOverride transaction = preparedTransaction.transaction();
			if (preparedTransaction.earlyExcludedReason() != null) {
				resolutionsByTransactionId.put(
					transaction.getId(),
					Resolution.excluded(transaction.getCategory(), preparedTransaction.earlyExcludedReason())
				);
				continue;
			}

			String categoryName = transaction.getCategory().getName();
			TransactionExcludedReason postLabelExclusion = transactionExclusionPolicy.determinePostLabelExclusion(categoryName);
			if (postLabelExclusion != null) {
				resolutionsByTransactionId.put(
					transaction.getId(),
					Resolution.excluded(transaction.getCategory(), postLabelExclusion)
				);
				continue;
			}

			classifierCandidates.add(new FixedExpenseClassifier.Candidate(
				transaction.getId(),
				categoryName,
				preparedTransaction.normalizedMerchantText(),
				transaction.getTransactionDateTime().toLocalDate(),
				transaction.getTransactionBalance()
			));
			resolutionsByTransactionId.put(transaction.getId(), Resolution.included(transaction.getCategory()));
		}
		java.util.Map<Integer, ExpenseScope> expenseScopesByTransactionId = fixedExpenseClassifier.classify(classifierCandidates);

		List<TransactionAnalysisResult> analysisResults = preparedTransactions.stream()
			.map(preparedTransaction -> toAnalysisResult(
				preparedTransaction.transaction(),
				resolutionsByTransactionId.get(preparedTransaction.transaction().getId()),
				expenseScopesByTransactionId.get(preparedTransaction.transaction().getId()),
				analyzedAt
			))
			.toList();
		transactionAnalysisResultRepository.saveAll(analysisResults);

		persistWeeklySnapshots(user, analysisResults, analyzedAt);

		return transactionAnalysisMetadataRepository.save(TransactionAnalysisMetadata.builder()
			.user(user)
			.requestedMonths(requestedMonths)
			.windowStartAt(analysisWindow.windowStartAt())
			.windowEndAt(analysisWindow.windowEndAt())
			.transactionCount(currentSignature.transactionCount())
			.latestTransactionAt(currentSignature.latestTransactionAt())
			.analyzedAt(analyzedAt)
			.build());
	}

	private AnalysisWindow resolveCompletedMonthWindow(int requestedMonths) {
		YearMonth latestCompletedMonth = YearMonth.from(LocalDateTime.now(clock)).minusMonths(1);
		YearMonth startMonth = latestCompletedMonth.minusMonths(requestedMonths - 1L);
		return new AnalysisWindow(
			startMonth.atDay(1).atStartOfDay(),
			latestCompletedMonth.atEndOfMonth().atTime(LocalTime.MAX)
		);
	}

	private AnalysisWindow resolveMonthWindow(YearMonth month) {
		YearMonth targetMonth = month == null ? YearMonth.from(LocalDateTime.now(clock)) : month;
		return new AnalysisWindow(
			targetMonth.atDay(1).atStartOfDay(),
			targetMonth.atEndOfMonth().atTime(LocalTime.MAX)
		);
	}

	private List<TransactionOverride> loadTransactions(Integer userId, LocalDateTime fromDateTime, LocalDateTime toDateTime) {
		List<TransactionOverride> transactions = new ArrayList<>();
		transactions.addAll(accountTransactionOverrideRepository
			.findAllByAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(userId, fromDateTime, toDateTime));
		transactions.addAll(cardTransactionOverrideRepository
			.findAllByCard_WithdrawalAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(
				userId,
				fromDateTime,
				toDateTime
			));
		transactions.sort((left, right) -> right.getTransactionDateTime().compareTo(left.getTransactionDateTime()));
		return transactions;
	}

	private PreparedTransaction prepareTransaction(TransactionOverride transaction, Set<String> registeredAccountNos) {
		String merchantText = extractMerchantText(transaction);
		return new PreparedTransaction(
			transaction,
			normalizeMerchantText(merchantText),
			transactionExclusionPolicy.determinePreLabelExclusion(transaction, registeredAccountNos)
		);
	}

	private TransactionAnalysisResult toAnalysisResult(
		TransactionOverride transaction,
		Resolution resolution,
		ExpenseScope expenseScope,
		LocalDateTime analyzedAt
	) {
		return TransactionAnalysisResult.builder()
			.transaction(transaction)
			.category(resolution.category())
			.analysisIncluded(resolution.analysisIncluded())
			.excludedReason(resolution.excludedReason())
			.expenseScope(resolution.analysisIncluded() ? expenseScope : null)
			.rawLabel(null)
			.finalLabel(null)
			.confidence(null)
			.fallbackApplied(false)
			.thresholdUsed(null)
			.analyzedAt(analyzedAt)
			.build();
	}

	private void persistWeeklySnapshots(
		User user,
		List<TransactionAnalysisResult> analysisResults,
		LocalDateTime analyzedAt
	) {
		List<Entry> entries = analysisResults.stream()
			.filter(TransactionAnalysisResult::isAnalysisIncluded)
			.map(result -> new Entry(
				result.getCategory(),
				result.getExpenseScope(),
				result.getTransaction().getTransactionDateTime().toLocalDate(),
				result.getTransaction().getTransactionBalance()
			))
			.toList();
		List<WeeklySnapshot> weeklySnapshots = weeklyEssentialAllocator.allocate(
			WEEKLY_ESSENTIAL_BASE_MONTHS,
			entries,
			analyzedAt
		);

		List<EssentialExpenseSnapshotWeek> snapshotEntities = new ArrayList<>();
		List<EssentialExpenseSnapshotWeekCategory> categoryEntities = new ArrayList<>();
		for (WeeklySnapshot weeklySnapshot : weeklySnapshots) {
			EssentialExpenseSnapshotWeek snapshotEntity = EssentialExpenseSnapshotWeek.builder()
				.user(user)
				.yearWeek(weeklySnapshot.yearWeek())
				.weekStartDate(weeklySnapshot.weekStartDate())
				.weekEndDate(weeklySnapshot.weekEndDate())
				.baseMonths(weeklySnapshot.baseMonths())
				.essentialAmount(weeklySnapshot.essentialAmount())
				.fixedEssentialAmount(weeklySnapshot.fixedEssentialAmount())
				.variableEssentialAmount(weeklySnapshot.variableEssentialAmount())
				.analyzedAt(weeklySnapshot.analyzedAt())
				.build();
			snapshotEntities.add(snapshotEntity);

			for (WeeklyEssentialAllocator.CategoryAllocation categoryAllocation : weeklySnapshot.categoryAllocations()) {
				categoryEntities.add(EssentialExpenseSnapshotWeekCategory.builder()
					.snapshotWeek(snapshotEntity)
					.category(categoryAllocation.category())
					.expenseScope(categoryAllocation.expenseScope())
					.amount(categoryAllocation.amount())
					.build());
			}
		}
		if (!snapshotEntities.isEmpty()) {
			essentialExpenseSnapshotWeekRepository.saveAll(snapshotEntities);
		}
		if (!categoryEntities.isEmpty()) {
			essentialExpenseSnapshotWeekCategoryRepository.saveAll(categoryEntities);
		}
	}

	private String extractMerchantText(TransactionOverride transaction) {
		if (transaction instanceof CardTransactionOverride cardTransaction) {
			return cardTransaction.getMerchant().getName();
		}
		if (transaction instanceof AccountTransactionOverride accountTransaction && hasText(accountTransaction.getTransactionSummary())) {
			return accountTransaction.getTransactionSummary();
		}
		return transaction.getTransactionMemo();
	}

	private String normalizeMerchantText(String merchantText) {
		if (!hasText(merchantText)) {
			return "";
		}
		return merchantText.trim().replaceAll("\\s+", " ");
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private int normalizeRequestedMonths(Integer months) {
		if (months == null || months <= 0) {
			return DEFAULT_MONTHS;
		}
		return months;
	}

	private record AnalysisWindow(
		LocalDateTime windowStartAt,
		LocalDateTime windowEndAt
	) {
	}

	private record AnalysisSignature(
		Integer transactionCount,
		LocalDateTime latestTransactionAt
	) {
		private static AnalysisSignature from(List<TransactionOverride> transactions) {
			LocalDateTime latestTransactionAt = transactions.isEmpty() ? null : transactions.get(0).getTransactionDateTime();
			return new AnalysisSignature(transactions.size(), latestTransactionAt);
		}
	}

	private record PreparedTransaction(
		TransactionOverride transaction,
		String normalizedMerchantText,
		TransactionExcludedReason earlyExcludedReason
	) {
	}

	private record Resolution(
		Category category,
		boolean analysisIncluded,
		TransactionExcludedReason excludedReason
	) {

		private static Resolution excluded(Category category, TransactionExcludedReason excludedReason) {
			return new Resolution(category, false, excludedReason);
		}

		private static Resolution included(Category category) {
			return new Resolution(category, true, null);
		}
	}
}

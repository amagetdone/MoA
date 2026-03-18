package com.ssafy.amagetdone.domain.transaction.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.account.model.repository.CategoryRepository;
import com.ssafy.amagetdone.domain.budget.application.usecase.UpdateBudgetSpentUseCase;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeekCategory;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekCategoryRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.domain.transaction.infra.client.MerchantInferenceApiClient;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.MerchantInferenceDto;
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
	private static final String DEFAULT_CATEGORY_NAME = "기타소비";

	private final UserRepository userRepository;
	private final AccountRepository accountRepository;
	private final CategoryRepository categoryRepository;
	private final AccountTransactionOverrideRepository accountTransactionOverrideRepository;
	private final CardTransactionOverrideRepository cardTransactionOverrideRepository;
	private final TransactionAnalysisResultRepository transactionAnalysisResultRepository;
	private final TransactionAnalysisMetadataRepository transactionAnalysisMetadataRepository;
	private final EssentialExpenseSnapshotWeekRepository essentialExpenseSnapshotWeekRepository;
	private final EssentialExpenseSnapshotWeekCategoryRepository essentialExpenseSnapshotWeekCategoryRepository;
	private final MerchantInferenceApiClient merchantInferenceApiClient;
	private final TransactionSyncService transactionSyncService;
	private final UpdateBudgetSpentUseCase updateBudgetSpentUseCase;
	private final TransactionExclusionPolicy transactionExclusionPolicy;
	private final FixedExpenseClassifier fixedExpenseClassifier;
	private final WeeklyEssentialAllocator weeklyEssentialAllocator;

	private Clock clock = Clock.systemDefaultZone();

	@Transactional
	public TransactionAnalysisMetadata analyzeTransactions(Integer userId, Integer months) {
		int requestedMonths = normalizeRequestedMonths(months);
		AnalysisWindow analysisWindow = resolveCompletedMonthWindow(requestedMonths);
		List<TransactionOverride> sourceTransactions = loadTransactions(
			userId,
			analysisWindow.windowStartAt(),
			analysisWindow.windowEndAt()
		);
		AnalysisSignature currentSignature = AnalysisSignature.from(sourceTransactions);

		Optional<TransactionAnalysisMetadata> latestMetadata = transactionAnalysisMetadataRepository
			.findTopByUser_IdAndRequestedMonthsOrderByAnalyzedAtDesc(userId, requestedMonths);
		if (latestMetadata.isPresent() && matches(latestMetadata.get(), analysisWindow, currentSignature)) {
			return latestMetadata.get();
		}

		User user = userRepository.getReferenceById(userId);
		LocalDateTime analyzedAt = LocalDateTime.now(clock);
		Set<String> registeredAccountNos = accountRepository.findAllByUser_Id(userId).stream()
			.map(Account::getAccountNo)
			.filter(this::hasText)
			.map(String::trim)
			.collect(Collectors.toSet());
		Map<String, Category> categoriesByName = categoryRepository.findAll().stream()
			.collect(Collectors.toMap(Category::getName, Function.identity(), (left, right) -> left, LinkedHashMap::new));
		Category defaultCategory = Optional.ofNullable(categoriesByName.get(DEFAULT_CATEGORY_NAME))
			.orElseGet(() -> categoryRepository.getReferenceById(4));
		TransactionAnalysisJob job = TransactionAnalysisJob.builder()
			.id(UUID.randomUUID().toString())
			.user(user)
			.requestedMonths(normalizeRequestedMonths(months))
			.status(TransactionAnalysisJobStatus.PENDING)
			.currentStep(TransactionAnalysisStep.SYNCING)
			.requestedAt(LocalDateTime.now(clock))
			.build();
		return transactionAnalysisJobRepository.save(job);
	}

	public TransactionAnalysisJob runAnalysis(String jobId, boolean forceFullSync) {
		TransactionAnalysisJob job = getJob(jobId);

		try {
			LocalDateTime now = LocalDateTime.now(clock);
			job.setStatus(TransactionAnalysisJobStatus.RUNNING);
			job.setStartedAt(now);
			job.setCurrentStep(TransactionAnalysisStep.SYNCING);
			transactionAnalysisJobRepository.save(job);

			Integer userId = job.getUser().getId();
			transactionSyncService.syncLatest(userId, job.getRequestedMonths(), forceFullSync);

			LocalDateTime analyzedAt = LocalDateTime.now(clock);
			LocalDateTime fromDateTime = analyzedAt.minusMonths(job.getRequestedMonths());
			LocalDateTime toDateTime = analyzedAt;
		Set<String> registeredAccountNos = accountRepository.findAllLinkedAccountsByUserId(userId).stream()
			.filter(Account::isActive)
			.map(Account::getAccountNo)
			.filter(this::hasText)
			.map(String::trim)
			.collect(Collectors.toSet());
			Map<String, Category> categoriesByName = categoryRepository.findAll().stream()
				.collect(Collectors.toMap(Category::getName, Function.identity(), (left, right) -> left, LinkedHashMap::new));
			Category defaultCategory = Optional.ofNullable(categoriesByName.get(DEFAULT_CATEGORY_NAME))
				.orElseGet(() -> categoryRepository.getReferenceById(4));

		List<PreparedTransaction> preparedTransactions = sourceTransactions.stream()
			.map(transaction -> prepareTransaction(transaction, registeredAccountNos))
			.toList();
		Map<String, MerchantInferenceDto.Result> labelsByText = labelTransactions(preparedTransactions);

		List<FixedExpenseClassifier.Candidate> classifierCandidates = new ArrayList<>();
		Map<Integer, Resolution> resolutionsByTransactionId = new LinkedHashMap<>();
		for (PreparedTransaction preparedTransaction : preparedTransactions) {
			if (preparedTransaction.earlyExcludedReason() != null) {
				resolutionsByTransactionId.put(
					preparedTransaction.transaction().getId(),
					Resolution.excluded(preparedTransaction.transaction().getCategory(), preparedTransaction.earlyExcludedReason(), null)
				);
				continue;
			}

			MerchantInferenceDto.Result label = labelsByText.get(preparedTransaction.normalizedMerchantText());
			String finalLabel = label != null ? label.finalLabel() : DEFAULT_CATEGORY_NAME;
			Category resolvedCategory = categoriesByName.getOrDefault(finalLabel, defaultCategory);
			TransactionExcludedReason postLabelExclusion = transactionExclusionPolicy.determinePostLabelExclusion(finalLabel);
			if (postLabelExclusion != null) {
				resolutionsByTransactionId.put(
					preparedTransaction.transaction().getId(),
					Resolution.excluded(resolvedCategory, postLabelExclusion, label)
				);
				continue;
			}

			classifierCandidates.add(new FixedExpenseClassifier.Candidate(
				preparedTransaction.transaction().getId(),
				finalLabel,
				preparedTransaction.normalizedMerchantText(),
				preparedTransaction.transaction().getTransactionDateTime().toLocalDate(),
				preparedTransaction.transaction().getTransactionBalance()
			));
			resolutionsByTransactionId.put(
				preparedTransaction.transaction().getId(),
				Resolution.included(resolvedCategory, label)
			);
		}
		Map<Integer, ExpenseScope> expenseScopesByTransactionId = fixedExpenseClassifier.classify(classifierCandidates);

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
			job.setCurrentStep(TransactionAnalysisStep.BUILDING_SNAPSHOTS);
			transactionAnalysisJobRepository.save(job);
			persistWeeklySnapshots(job.getUser(), analysisResults, analyzedAt);
			updateBudgetSpentUseCase.execute(job.getUser(), analysisResults);

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

	private boolean matches(
		TransactionAnalysisMetadata metadata,
		AnalysisWindow analysisWindow,
		AnalysisSignature analysisSignature
	) {
		return metadata.getWindowStartAt().equals(analysisWindow.windowStartAt())
			&& metadata.getWindowEndAt().equals(analysisWindow.windowEndAt())
			&& metadata.getTransactionCount().equals(analysisSignature.transactionCount())
			&& java.util.Objects.equals(metadata.getLatestTransactionAt(), analysisSignature.latestTransactionAt());
	}

	private AnalysisWindow resolveCompletedMonthWindow(int requestedMonths) {
		YearMonth latestCompletedMonth = YearMonth.from(LocalDateTime.now(clock)).minusMonths(1);
		YearMonth startMonth = latestCompletedMonth.minusMonths(requestedMonths - 1L);
		return new AnalysisWindow(
			startMonth.atDay(1).atStartOfDay(),
			latestCompletedMonth.atEndOfMonth().atTime(LocalTime.MAX)
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
			).stream()
			.filter(transaction -> transaction.getCard().getWithdrawalAccount().isActive())
			.toList());
		transactions.sort((left, right) -> right.getTransactionDateTime().compareTo(left.getTransactionDateTime()));
		return transactions;
	}

	private PreparedTransaction prepareTransaction(TransactionOverride transaction, Set<String> registeredAccountNos) {
		String merchantText = extractMerchantText(transaction);
		return new PreparedTransaction(
			transaction,
			merchantText,
			normalizeMerchantText(merchantText),
			transactionExclusionPolicy.determinePreLabelExclusion(transaction, registeredAccountNos)
		);
	}

	private Map<String, MerchantInferenceDto.Result> labelTransactions(List<PreparedTransaction> preparedTransactions) {
		List<String> merchantTexts = preparedTransactions.stream()
			.filter(preparedTransaction -> preparedTransaction.earlyExcludedReason() == null)
			.map(PreparedTransaction::normalizedMerchantText)
			.filter(this::hasText)
			.distinct()
			.toList();
		if (merchantTexts.isEmpty()) {
			return Map.of();
		}

		return merchantInferenceApiClient.labelMerchants(merchantTexts).results().stream()
			.collect(Collectors.toMap(
				result -> normalizeMerchantText(
					hasText(result.normalizedMerchantText()) ? result.normalizedMerchantText() : result.merchantText()
				),
				Function.identity(),
				(left, right) -> left,
				LinkedHashMap::new
			));
	}

	private TransactionAnalysisResult toAnalysisResult(
		TransactionOverride transaction,
		Resolution resolution,
		ExpenseScope expenseScope,
		LocalDateTime analyzedAt
	) {
		MerchantInferenceDto.Result label = resolution.label();
		return TransactionAnalysisResult.builder()
			.transaction(transaction)
			.category(resolution.category())
			.analysisIncluded(resolution.analysisIncluded())
			.excludedReason(resolution.excludedReason())
			.expenseScope(resolution.analysisIncluded() ? expenseScope : null)
			.rawLabel(label != null ? label.rawLabel() : null)
			.finalLabel(label != null ? label.finalLabel() : null)
			.confidence(label != null ? label.confidence() : null)
			.fallbackApplied(label != null && Boolean.TRUE.equals(label.fallbackApplied()))
			.thresholdUsed(label != null ? label.thresholdUsed() : null)
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
		String merchantText,
		String normalizedMerchantText,
		TransactionExcludedReason earlyExcludedReason
	) {
	}

	private record Resolution(
		Category category,
		boolean analysisIncluded,
		TransactionExcludedReason excludedReason,
		MerchantInferenceDto.Result label
	) {

		private static Resolution excluded(
			Category category,
			TransactionExcludedReason excludedReason,
			MerchantInferenceDto.Result label
		) {
			return new Resolution(category, false, excludedReason, label);
		}

		private static Resolution included(Category category, MerchantInferenceDto.Result label) {
			return new Resolution(category, true, null, label);
		}
	}
}

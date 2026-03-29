package com.ssafy.amagetdone.domain.transaction.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Card;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.entity.Merchant;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.account.model.repository.CardRepository;
import com.ssafy.amagetdone.domain.account.model.repository.CategoryRepository;
import com.ssafy.amagetdone.domain.account.model.repository.MerchantRepository;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.domain.transaction.infra.client.SsafyCardTransactionApiClient;
import com.ssafy.amagetdone.domain.transaction.infra.client.SsafyDemandDepositTransactionApiClient;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.CardTransactionDto;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.DemandDepositTransactionDto;
import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.repository.AccountTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.CardTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.notification.application.event.TransactionSyncedEvent;
import com.ssafy.amagetdone.domain.transaction.support.util.TransactionDateRangeFactory;
import com.ssafy.amagetdone.domain.transaction.support.util.TransactionDateRangeFactory.DateRange;
import com.ssafy.amagetdone.domain.transaction.support.util.TransactionMerchantTextResolver;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TransactionSyncService {

	private static final int DEFAULT_CATEGORY_ID = 4;
	private static final String DEMAND_DEPOSIT_TRANSACTION_TYPE = "A";
	private static final String DESCENDING_ORDER = "DESC";
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

	private final UserRepository userRepository;
	private final AccountRepository accountRepository;
	private final CardRepository cardRepository;
	private final CategoryRepository categoryRepository;
	private final MerchantRepository merchantRepository;
	private final AccountTransactionOverrideRepository accountTransactionOverrideRepository;
	private final CardTransactionOverrideRepository cardTransactionOverrideRepository;
	private final SsafyDemandDepositTransactionApiClient demandDepositTransactionApiClient;
	private final SsafyCardTransactionApiClient cardTransactionApiClient;
	private final TransactionDateRangeFactory dateRangeFactory;
	private final TransactionCategoryAssignmentService transactionCategoryAssignmentService;
	private final ApplicationEventPublisher applicationEventPublisher;

	public SyncResult syncLatest(Integer userId, int months, boolean forceFullSync) {
		acquireUserSyncLock(userId);

		List<Account> linkedAccounts = accountRepository.findAllLinkedAccountsByUserId(userId).stream()
			.filter(Account::isActive)
			.toList();
		int insertedAccountCount = linkedAccounts.stream()
			.mapToInt(account -> syncAccount(account, months, forceFullSync))
			.sum();
		int insertedCardCount = cardRepository
			.findAllByWithdrawalAccount_User_IdAndWithdrawalAccount_VerifiedTrueAndWithdrawalAccount_ActiveTrue(userId)
			.stream()
			.mapToInt(card -> syncCard(card, months, forceFullSync))
			.sum();
		return new SyncResult(insertedAccountCount, insertedCardCount);
	}

	public SyncResult syncRange(Integer userId, LocalDate fromDate, LocalDate toDate) {
		acquireUserSyncLock(userId);

		DateRange dateRange = dateRangeFactory.resolve(fromDate, toDate);
		List<Account> linkedAccounts = accountRepository.findAllLinkedAccountsByUserId(userId).stream()
			.filter(Account::isActive)
			.toList();
		int insertedAccountCount = linkedAccounts.stream()
			.mapToInt(account -> syncAccount(account, dateRange))
			.sum();
		int insertedCardCount = cardRepository
			.findAllByWithdrawalAccount_User_IdAndWithdrawalAccount_VerifiedTrueAndWithdrawalAccount_ActiveTrue(userId)
			.stream()
			.mapToInt(card -> syncCard(card, dateRange))
			.sum();
		return new SyncResult(insertedAccountCount, insertedCardCount);
	}

	private void acquireUserSyncLock(Integer userId) {
		if (userId == null) {
			return;
		}
		userRepository.findByIdForUpdate(userId);
	}

	private int syncAccount(Account account, int months, boolean forceFullSync) {
		Optional<AccountTransactionOverride> latestLocal = forceFullSync
			? Optional.empty()
			: accountTransactionOverrideRepository.findTopByAccount_IdOrderByTransactionDateTimeDesc(account.getId());
		boolean shouldPublishEvents = !forceFullSync && latestLocal.isPresent();
		DateRange dateRange = dateRangeFactory.resolve(
			months,
			latestLocal.map(transaction -> transaction.getTransactionDateTime().toLocalDate()).orElse(null),
			forceFullSync
		);
		DemandDepositTransactionDto.Response response = demandDepositTransactionApiClient.inquireTransactions(
			new DemandDepositTransactionDto.Request(
				account.getUser().getUserKey(),
				account.getAccountNo(),
				dateRange.startDateText(),
				dateRange.endDateText(),
				DEMAND_DEPOSIT_TRANSACTION_TYPE,
				DESCENDING_ORDER
			)
		);
		if (response.transactions().isEmpty()) {
			return 0;
		}
		if (latestLocal.isPresent() && matchesLatestAccount(latestLocal.get(), response.transactions().get(0))) {
			return 0;
		}

		List<DemandDepositTransactionDto.Item> newItems = response.transactions().stream()
			.filter(item -> !accountTransactionOverrideRepository.existsByAccount_IdAndTransactionUniqueNo(
				account.getId(),
				item.transactionUniqueNo()
			))
			.toList();
		if (newItems.isEmpty()) {
			return 0;
		}
		Map<String, Category> assignedCategories = transactionCategoryAssignmentService.assignCategories(
			newItems.stream()
				.map(TransactionMerchantTextResolver::resolve)
				.toList()
		);
		Category defaultCategory = categoryRepository.getReferenceById(DEFAULT_CATEGORY_ID);
		List<AccountTransactionOverride> transactions = newItems.stream()
			.map(item -> toAccountTransaction(
				account,
				resolveCategory(assignedCategories, TransactionMerchantTextResolver.resolve(item), defaultCategory),
				item
			))
			.toList();

		accountTransactionOverrideRepository.saveAll(transactions);
		if (shouldPublishEvents) {
			publishAccountTransactionEvents(account.getUser().getId(), transactions);
		}
		return transactions.size();
	}

	private int syncAccount(Account account, DateRange dateRange) {
		DemandDepositTransactionDto.Response response = demandDepositTransactionApiClient.inquireTransactions(
			new DemandDepositTransactionDto.Request(
				account.getUser().getUserKey(),
				account.getAccountNo(),
				dateRange.startDateText(),
				dateRange.endDateText(),
				DEMAND_DEPOSIT_TRANSACTION_TYPE,
				DESCENDING_ORDER
			)
		);
		if (response.transactions().isEmpty()) {
			return 0;
		}

		List<DemandDepositTransactionDto.Item> newItems = response.transactions().stream()
			.filter(item -> !accountTransactionOverrideRepository.existsByAccount_IdAndTransactionUniqueNo(
				account.getId(),
				item.transactionUniqueNo()
			))
			.toList();
		if (newItems.isEmpty()) {
			return 0;
		}
		Map<String, Category> assignedCategories = transactionCategoryAssignmentService.assignCategories(
			newItems.stream()
				.map(TransactionMerchantTextResolver::resolve)
				.toList()
		);
		Category defaultCategory = categoryRepository.getReferenceById(DEFAULT_CATEGORY_ID);
		List<AccountTransactionOverride> transactions = newItems.stream()
			.map(item -> toAccountTransaction(
				account,
				resolveCategory(assignedCategories, TransactionMerchantTextResolver.resolve(item), defaultCategory),
				item
			))
			.toList();

		accountTransactionOverrideRepository.saveAll(transactions);
		publishAccountTransactionEvents(account.getUser().getId(), transactions);
		return transactions.size();
	}

	private int syncCard(Card card, int months, boolean forceFullSync) {
		Optional<CardTransactionOverride> latestLocal = forceFullSync
			? Optional.empty()
			: cardTransactionOverrideRepository.findTopByCard_CardUniqueNoOrderByTransactionDateTimeDesc(card.getCardUniqueNo());
		boolean shouldPublishEvents = !forceFullSync && latestLocal.isPresent();
		DateRange dateRange = dateRangeFactory.resolve(
			months,
			latestLocal.map(transaction -> transaction.getTransactionDateTime().toLocalDate()).orElse(null),
			forceFullSync
		);
		CardTransactionDto.Response response = cardTransactionApiClient.inquireTransactions(
			new CardTransactionDto.Request(
				card.getWithdrawalAccount().getUser().getUserKey(),
				card.getCardNo(),
				card.getCvc(),
				dateRange.startDateText(),
				dateRange.endDateText()
			)
		);
		if (response.transactions().isEmpty()) {
			return 0;
		}
		if (latestLocal.isPresent() && matchesLatestCard(latestLocal.get(), response.transactions().get(0))) {
			return 0;
		}

		List<CardTransactionDto.Item> newItems = response.transactions().stream()
			.filter(item -> !cardTransactionOverrideRepository.existsByCard_CardUniqueNoAndTransactionUniqueNo(
				card.getCardUniqueNo(),
				item.transactionUniqueNo()
			))
			.toList();
		if (newItems.isEmpty()) {
			return 0;
		}
		Map<String, Category> assignedCategories = transactionCategoryAssignmentService.assignCategories(
			newItems.stream()
				.map(TransactionMerchantTextResolver::resolve)
				.toList()
		);
		Category defaultCategory = categoryRepository.getReferenceById(DEFAULT_CATEGORY_ID);
		List<CardTransactionOverride> transactions = newItems.stream()
			.map(item -> toCardTransaction(
				card,
				resolveCategory(assignedCategories, TransactionMerchantTextResolver.resolve(item), defaultCategory),
				item
			))
			.toList();

		cardTransactionOverrideRepository.saveAll(transactions);
		if (shouldPublishEvents) {
			publishCardTransactionEvents(card.getWithdrawalAccount().getUser().getId(), transactions);
		}
		return transactions.size();
	}

	private int syncCard(Card card, DateRange dateRange) {
		CardTransactionDto.Response response = cardTransactionApiClient.inquireTransactions(
			new CardTransactionDto.Request(
				card.getWithdrawalAccount().getUser().getUserKey(),
				card.getCardNo(),
				card.getCvc(),
				dateRange.startDateText(),
				dateRange.endDateText()
			)
		);
		if (response.transactions().isEmpty()) {
			return 0;
		}

		List<CardTransactionDto.Item> newItems = response.transactions().stream()
			.filter(item -> !cardTransactionOverrideRepository.existsByCard_CardUniqueNoAndTransactionUniqueNo(
				card.getCardUniqueNo(),
				item.transactionUniqueNo()
			))
			.toList();
		if (newItems.isEmpty()) {
			return 0;
		}
		Map<String, Category> assignedCategories = transactionCategoryAssignmentService.assignCategories(
			newItems.stream()
				.map(TransactionMerchantTextResolver::resolve)
				.toList()
		);
		Category defaultCategory = categoryRepository.getReferenceById(DEFAULT_CATEGORY_ID);
		List<CardTransactionOverride> transactions = newItems.stream()
			.map(item -> toCardTransaction(
				card,
				resolveCategory(assignedCategories, TransactionMerchantTextResolver.resolve(item), defaultCategory),
				item
			))
			.toList();

		cardTransactionOverrideRepository.saveAll(transactions);
		publishCardTransactionEvents(card.getWithdrawalAccount().getUser().getId(), transactions);
		return transactions.size();
	}

	private void publishAccountTransactionEvents(Integer userId, List<AccountTransactionOverride> transactions) {
		if (userId == null) {
			return;
		}
		transactions.forEach(transaction -> applicationEventPublisher.publishEvent(new TransactionSyncedEvent(
			userId,
			"ACCOUNT",
			String.valueOf(transaction.getTransactionUniqueNo()),
			transaction.getTransactionBalance(),
			transaction.getTransactionTypeName(),
			transaction.getTransactionSummary(),
			transaction.getTransactionDateTime()
		)));
	}

	private void publishCardTransactionEvents(Integer userId, List<CardTransactionOverride> transactions) {
		if (userId == null) {
			return;
		}
		transactions.forEach(transaction -> applicationEventPublisher.publishEvent(new TransactionSyncedEvent(
			userId,
			"CARD",
			String.valueOf(transaction.getTransactionUniqueNo()),
			transaction.getTransactionBalance(),
			null,
			transaction.getMerchant() == null ? transaction.getTransactionMemo() : transaction.getMerchant().getName(),
			transaction.getTransactionDateTime()
		)));
	}

	private boolean matchesLatestAccount(AccountTransactionOverride latestLocal, DemandDepositTransactionDto.Item latestRemote) {
		return Objects.equals(latestLocal.getTransactionUniqueNo(), latestRemote.transactionUniqueNo())
			&& Objects.equals(latestLocal.getTransactionDateTime(), toDateTime(latestRemote.transactionDate(), latestRemote.transactionTime()))
			&& Objects.equals(latestLocal.getTransactionBalance(), latestRemote.transactionBalance());
	}

	private boolean matchesLatestCard(CardTransactionOverride latestLocal, CardTransactionDto.Item latestRemote) {
		return Objects.equals(latestLocal.getTransactionUniqueNo(), latestRemote.transactionUniqueNo())
			&& Objects.equals(latestLocal.getTransactionDateTime(), toDateTime(latestRemote.transactionDate(), latestRemote.transactionTime()))
			&& Objects.equals(latestLocal.getTransactionBalance(), latestRemote.transactionBalance());
	}

	private AccountTransactionOverride toAccountTransaction(
		Account account,
		Category defaultCategory,
		DemandDepositTransactionDto.Item item
	) {
		String memo = hasText(item.transactionMemo()) ? item.transactionMemo() : item.transactionSummary();
		return AccountTransactionOverride.builder()
			.category(defaultCategory)
			.transactionDateTime(toDateTime(item.transactionDate(), item.transactionTime()))
			.transactionBalance(item.transactionBalance())
			.transactionMemo(memo)
			.transactionUniqueNo(item.transactionUniqueNo())
			.transactionType(parseInteger(item.transactionType()))
			.transactionTypeName(item.transactionTypeName())
			.transactionAfterBalance(item.transactionAfterBalance())
			.transactionSummary(item.transactionSummary())
			.account(account)
			.build();
	}

	private CardTransactionOverride toCardTransaction(
		Card card,
		Category defaultCategory,
		CardTransactionDto.Item item
	) {
		Merchant merchant = resolveMerchant(item.merchantId(), item.merchantName());
		return CardTransactionOverride.builder()
			.category(defaultCategory)
			.transactionDateTime(toDateTime(item.transactionDate(), item.transactionTime()))
			.transactionBalance(item.transactionBalance())
			.transactionMemo(item.merchantName())
			.transactionUniqueNo(item.transactionUniqueNo())
			.merchant(merchant)
			.cardStatus(toBoolean(item.cardStatus()))
			.billStatementsYn(toBoolean(item.billStatementsYn()))
			.billStatementsStatus(item.billStatementsStatus())
			.card(card)
			.build();
	}

	private Merchant resolveMerchant(Long merchantId, String merchantName) {
		int resolvedMerchantId = merchantId == null
			? buildTransactionId("MERCHANT", merchantName, 0L, "", "")
			: Math.toIntExact(merchantId);
		return merchantRepository.findById(resolvedMerchantId)
			.orElseGet(() -> merchantRepository.save(Merchant.builder()
				.id(resolvedMerchantId)
				.name(merchantName)
				.build()));
	}

	private LocalDateTime toDateTime(String transactionDate, String transactionTime) {
		return LocalDateTime.of(
			LocalDate.parse(transactionDate, DATE_FORMATTER),
			java.time.LocalTime.parse(normalizeTime(transactionTime), TIME_FORMATTER)
		);
	}

	private String normalizeTime(String transactionTime) {
		if (transactionTime == null) {
			return "000000";
		}
		return transactionTime.length() == 6 ? transactionTime : String.format("%1$" + 6 + "s", transactionTime).replace(' ', '0');
	}

	private int parseInteger(String value) {
		if (value == null || value.isBlank()) {
			return 0;
		}
		return Integer.parseInt(value);
	}

	private boolean toBoolean(String value) {
		if (value == null) {
			return false;
		}
		return "Y".equalsIgnoreCase(value)
			|| "TRUE".equalsIgnoreCase(value)
			|| "1".equals(value)
			|| "승인".equals(value);
	}

	private int buildTransactionId(String sourceType, String sourceKey, Long transactionUniqueNo, String transactionDate, String transactionTime) {
		return Math.abs(Objects.hash(sourceType, sourceKey, transactionUniqueNo, transactionDate, transactionTime));
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private Category resolveCategory(Map<String, Category> assignedCategories, String sourceText, Category defaultCategory) {
		if (assignedCategories == null || assignedCategories.isEmpty()) {
			return defaultCategory;
		}
		return assignedCategories.getOrDefault(sourceText, defaultCategory);
	}

	public record SyncResult(
		int insertedAccountCount,
		int insertedCardCount
	) {
	}
}

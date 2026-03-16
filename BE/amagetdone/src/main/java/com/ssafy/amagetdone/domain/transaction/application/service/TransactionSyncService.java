package com.ssafy.amagetdone.domain.transaction.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.ssafy.amagetdone.domain.asset.model.entity.Account;
import com.ssafy.amagetdone.domain.asset.model.entity.Card;
import com.ssafy.amagetdone.domain.asset.model.entity.Category;
import com.ssafy.amagetdone.domain.asset.model.entity.Merchant;
import com.ssafy.amagetdone.domain.asset.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.asset.model.repository.CardRepository;
import com.ssafy.amagetdone.domain.asset.model.repository.CategoryRepository;
import com.ssafy.amagetdone.domain.asset.model.repository.MerchantRepository;
import com.ssafy.amagetdone.domain.transaction.infra.client.SsafyCardTransactionApiClient;
import com.ssafy.amagetdone.domain.transaction.infra.client.SsafyDemandDepositTransactionApiClient;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.CardTransactionDto;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.DemandDepositTransactionDto;
import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.repository.AccountTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.CardTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.support.util.TransactionDateRangeFactory;
import com.ssafy.amagetdone.domain.transaction.support.util.TransactionDateRangeFactory.DateRange;

import lombok.RequiredArgsConstructor;

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

	private final AccountRepository accountRepository;
	private final CardRepository cardRepository;
	private final CategoryRepository categoryRepository;
	private final MerchantRepository merchantRepository;
	private final AccountTransactionOverrideRepository accountTransactionOverrideRepository;
	private final CardTransactionOverrideRepository cardTransactionOverrideRepository;
	private final SsafyDemandDepositTransactionApiClient demandDepositTransactionApiClient;
	private final SsafyCardTransactionApiClient cardTransactionApiClient;
	private final TransactionDateRangeFactory dateRangeFactory;

	public SyncResult syncLatest(Integer userId, int months, boolean forceFullSync) {
		int insertedAccountCount = accountRepository.findAllByUser_Id(userId).stream()
			.mapToInt(account -> syncAccount(account, months, forceFullSync))
			.sum();
		int insertedCardCount = cardRepository.findAllByWithdrawalAccount_User_Id(userId).stream()
			.mapToInt(card -> syncCard(card, months, forceFullSync))
			.sum();
		return new SyncResult(insertedAccountCount, insertedCardCount);
	}

	private int syncAccount(Account account, int months, boolean forceFullSync) {
		Optional<AccountTransactionOverride> latestLocal = forceFullSync
			? Optional.empty()
			: accountTransactionOverrideRepository.findTopByAccount_IdOrderByTransactionDateTimeDesc(account.getId());
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

		Category defaultCategory = categoryRepository.getReferenceById(DEFAULT_CATEGORY_ID);
		List<AccountTransactionOverride> transactions = response.transactions().stream()
			.filter(item -> !accountTransactionOverrideRepository.existsByAccount_IdAndTransactionUniqueNo(
				account.getId(),
				item.transactionUniqueNo()
			))
			.map(item -> toAccountTransaction(account, defaultCategory, item))
			.toList();
		if (transactions.isEmpty()) {
			return 0;
		}

		accountTransactionOverrideRepository.saveAll(transactions);
		return transactions.size();
	}

	private int syncCard(Card card, int months, boolean forceFullSync) {
		Optional<CardTransactionOverride> latestLocal = forceFullSync
			? Optional.empty()
			: cardTransactionOverrideRepository.findTopByCard_CardUniqueNoOrderByTransactionDateTimeDesc(card.getCardUniqueNo());
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

		Category defaultCategory = categoryRepository.getReferenceById(DEFAULT_CATEGORY_ID);
		List<CardTransactionOverride> transactions = response.transactions().stream()
			.filter(item -> !cardTransactionOverrideRepository.existsByCard_CardUniqueNoAndTransactionUniqueNo(
				card.getCardUniqueNo(),
				item.transactionUniqueNo()
			))
			.map(item -> toCardTransaction(card, defaultCategory, item))
			.toList();
		if (transactions.isEmpty()) {
			return 0;
		}

		cardTransactionOverrideRepository.saveAll(transactions);
		return transactions.size();
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
			.id(buildTransactionId("ACCOUNT", account.getId(), item.transactionUniqueNo(), item.transactionDate(), item.transactionTime()))
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
			.id(buildTransactionId("CARD", String.valueOf(card.getCardUniqueNo()), item.transactionUniqueNo(), item.transactionDate(), item.transactionTime()))
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

	public record SyncResult(
		int insertedAccountCount,
		int insertedCardCount
	) {
	}
}

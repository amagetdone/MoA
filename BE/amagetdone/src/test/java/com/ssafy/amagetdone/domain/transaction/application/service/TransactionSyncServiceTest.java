package com.ssafy.amagetdone.domain.transaction.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Card;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.entity.Merchant;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.account.model.repository.CardRepository;
import com.ssafy.amagetdone.domain.account.model.repository.CategoryRepository;
import com.ssafy.amagetdone.domain.account.model.repository.MerchantRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.vo.UserStatus;
import com.ssafy.amagetdone.domain.transaction.infra.client.SsafyCardTransactionApiClient;
import com.ssafy.amagetdone.domain.transaction.infra.client.SsafyDemandDepositTransactionApiClient;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.CardTransactionDto;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.DemandDepositTransactionDto;
import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.repository.AccountTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.CardTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.support.util.TransactionDateRangeFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TransactionSyncServiceTest {

	private static final int DEFAULT_CATEGORY_ID = 4;

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private CardRepository cardRepository;

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private MerchantRepository merchantRepository;

	@Mock
	private AccountTransactionOverrideRepository accountTransactionOverrideRepository;

	@Mock
	private CardTransactionOverrideRepository cardTransactionOverrideRepository;

	@Mock
	private SsafyDemandDepositTransactionApiClient demandDepositTransactionApiClient;

	@Mock
	private SsafyCardTransactionApiClient cardTransactionApiClient;

	@Mock
	private TransactionDateRangeFactory dateRangeFactory;

	@Test
	void syncSeedsAccountTransactionsWhenNoLocalTransactionExists() {
		TransactionSyncService service = new TransactionSyncService(
			accountRepository,
			cardRepository,
			categoryRepository,
			merchantRepository,
			accountTransactionOverrideRepository,
			cardTransactionOverrideRepository,
			demandDepositTransactionApiClient,
			cardTransactionApiClient,
			dateRangeFactory
		);
		User user = user(1, "user-key-1");
		Account account = account(user, "ACC-1", "111-222-333");
		Category defaultCategory = Category.create(DEFAULT_CATEGORY_ID, "기타서비스");
		TransactionDateRangeFactory.DateRange window = new TransactionDateRangeFactory.DateRange(
			LocalDate.of(2025, 10, 1),
			LocalDate.of(2026, 3, 13)
		);

		given(accountRepository.findAllByUser_Id(1)).willReturn(List.of(account));
		given(cardRepository.findAllByWithdrawalAccount_User_Id(1)).willReturn(List.of());
		given(dateRangeFactory.resolve(6, null, false)).willReturn(window);
		given(accountTransactionOverrideRepository.findTopByAccount_IdOrderByTransactionDateTimeDesc("ACC-1"))
			.willReturn(Optional.empty());
		given(demandDepositTransactionApiClient.inquireTransactions(any())).willReturn(
			new DemandDepositTransactionDto.Response(
				1,
				List.of(accountItem(1001L, "20260312", "103000", "2", "출금", "444-555", 15000L, 50000L, "점심", ""))
			)
		);
		given(categoryRepository.getReferenceById(DEFAULT_CATEGORY_ID)).willReturn(defaultCategory);

		service.syncLatest(1, 6, false);

		ArgumentCaptor<DemandDepositTransactionDto.Request> requestCaptor = ArgumentCaptor.forClass(
			DemandDepositTransactionDto.Request.class
		);
		verify(demandDepositTransactionApiClient).inquireTransactions(requestCaptor.capture());
		assertEquals("user-key-1", requestCaptor.getValue().userKey());
		assertEquals("111-222-333", requestCaptor.getValue().accountNo());
		assertEquals("20251001", requestCaptor.getValue().startDate());
		assertEquals("20260313", requestCaptor.getValue().endDate());
		assertEquals("A", requestCaptor.getValue().transactionType());
		assertEquals("DESC", requestCaptor.getValue().orderByType());

		ArgumentCaptor<List<AccountTransactionOverride>> saveCaptor = listCaptor();
		verify(accountTransactionOverrideRepository).saveAll(saveCaptor.capture());
		assertEquals(1, saveCaptor.getValue().size());
		AccountTransactionOverride saved = saveCaptor.getValue().get(0);
		assertEquals(account, saved.getAccount());
		assertEquals(defaultCategory, saved.getCategory());
		assertEquals(1001L, saved.getTransactionUniqueNo());
		assertEquals(LocalDateTime.of(2026, 3, 12, 10, 30), saved.getTransactionDateTime());
		assertEquals(15000L, saved.getTransactionBalance());
	}

	@Test
	void syncSkipsAccountSourceWhenNewestRemoteMatchesLocal() {
		TransactionSyncService service = new TransactionSyncService(
			accountRepository,
			cardRepository,
			categoryRepository,
			merchantRepository,
			accountTransactionOverrideRepository,
			cardTransactionOverrideRepository,
			demandDepositTransactionApiClient,
			cardTransactionApiClient,
			dateRangeFactory
		);
		User user = user(1, "user-key-1");
		Account account = account(user, "ACC-1", "111-222-333");
		Category defaultCategory = Category.create(DEFAULT_CATEGORY_ID, "기타서비스");
		AccountTransactionOverride latestLocal = accountTransaction(
			2001,
			defaultCategory,
			account,
			1002L,
			LocalDateTime.of(2026, 3, 12, 10, 30),
			15000L,
			50000L,
			"점심"
		);
		TransactionDateRangeFactory.DateRange window = new TransactionDateRangeFactory.DateRange(
			LocalDate.of(2026, 3, 12),
			LocalDate.of(2026, 3, 13)
		);

		given(accountRepository.findAllByUser_Id(1)).willReturn(List.of(account));
		given(cardRepository.findAllByWithdrawalAccount_User_Id(1)).willReturn(List.of());
		given(dateRangeFactory.resolve(6, LocalDate.of(2026, 3, 12), false)).willReturn(window);
		given(accountTransactionOverrideRepository.findTopByAccount_IdOrderByTransactionDateTimeDesc("ACC-1"))
			.willReturn(Optional.of(latestLocal));
		given(demandDepositTransactionApiClient.inquireTransactions(any())).willReturn(
			new DemandDepositTransactionDto.Response(
				1,
				List.of(accountItem(1002L, "20260312", "103000", "2", "출금", "444-555", 15000L, 50000L, "점심", ""))
			)
		);

		service.syncLatest(1, 6, false);

		verify(accountTransactionOverrideRepository, never()).saveAll(anyList());
	}

	@Test
	void syncReloadsAccountTransactionsFromLatestLocalDateWhenNewestRemoteDiffers() {
		TransactionSyncService service = new TransactionSyncService(
			accountRepository,
			cardRepository,
			categoryRepository,
			merchantRepository,
			accountTransactionOverrideRepository,
			cardTransactionOverrideRepository,
			demandDepositTransactionApiClient,
			cardTransactionApiClient,
			dateRangeFactory
		);
		User user = user(1, "user-key-1");
		Account account = account(user, "ACC-1", "111-222-333");
		Category defaultCategory = Category.create(DEFAULT_CATEGORY_ID, "기타서비스");
		AccountTransactionOverride latestLocal = accountTransaction(
			2001,
			defaultCategory,
			account,
			1002L,
			LocalDateTime.of(2026, 3, 10, 9, 0),
			15000L,
			50000L,
			"점심"
		);
		TransactionDateRangeFactory.DateRange window = new TransactionDateRangeFactory.DateRange(
			LocalDate.of(2026, 3, 10),
			LocalDate.of(2026, 3, 13)
		);

		given(accountRepository.findAllByUser_Id(1)).willReturn(List.of(account));
		given(cardRepository.findAllByWithdrawalAccount_User_Id(1)).willReturn(List.of());
		given(dateRangeFactory.resolve(6, LocalDate.of(2026, 3, 10), false)).willReturn(window);
		given(accountTransactionOverrideRepository.findTopByAccount_IdOrderByTransactionDateTimeDesc("ACC-1"))
			.willReturn(Optional.of(latestLocal));
		given(demandDepositTransactionApiClient.inquireTransactions(any())).willReturn(
			new DemandDepositTransactionDto.Response(
				2,
				List.of(
					accountItem(1003L, "20260312", "121500", "2", "출금", "444-555", 21000L, 29000L, "카페", ""),
					accountItem(1002L, "20260310", "090000", "2", "출금", "444-555", 15000L, 50000L, "점심", "")
				)
			)
		);
		given(accountTransactionOverrideRepository.existsByAccount_IdAndTransactionUniqueNo("ACC-1", 1003L)).willReturn(false);
		given(accountTransactionOverrideRepository.existsByAccount_IdAndTransactionUniqueNo("ACC-1", 1002L)).willReturn(true);
		given(categoryRepository.getReferenceById(DEFAULT_CATEGORY_ID)).willReturn(defaultCategory);

		service.syncLatest(1, 6, false);

		ArgumentCaptor<DemandDepositTransactionDto.Request> requestCaptor = ArgumentCaptor.forClass(
			DemandDepositTransactionDto.Request.class
		);
		verify(demandDepositTransactionApiClient).inquireTransactions(requestCaptor.capture());
		assertEquals("20260310", requestCaptor.getValue().startDate());
		assertEquals("20260313", requestCaptor.getValue().endDate());

		ArgumentCaptor<List<AccountTransactionOverride>> saveCaptor = listCaptor();
		verify(accountTransactionOverrideRepository).saveAll(saveCaptor.capture());
		assertEquals(1, saveCaptor.getValue().size());
		assertEquals(1003L, saveCaptor.getValue().get(0).getTransactionUniqueNo());
	}

	@Test
	void syncPersistsOnlyMissingCardTransactionsForEachCardSource() {
		TransactionSyncService service = new TransactionSyncService(
			accountRepository,
			cardRepository,
			categoryRepository,
			merchantRepository,
			accountTransactionOverrideRepository,
			cardTransactionOverrideRepository,
			demandDepositTransactionApiClient,
			cardTransactionApiClient,
			dateRangeFactory
		);
		User user = user(1, "user-key-1");
		Account withdrawalAccount = account(user, "ACC-1", "111-222-333");
		Card card = card(withdrawalAccount, 9999L, "1234-5678-9012-3456", "123");
		Category defaultCategory = Category.create(DEFAULT_CATEGORY_ID, "기타서비스");
		Merchant merchant = Merchant.builder().id(7).name("스타벅스").build();
		CardTransactionOverride latestLocal = cardTransaction(
			3001,
			defaultCategory,
			merchant,
			card,
			5001L,
			LocalDateTime.of(2026, 3, 10, 8, 30),
			6100L
		);
		TransactionDateRangeFactory.DateRange window = new TransactionDateRangeFactory.DateRange(
			LocalDate.of(2026, 3, 10),
			LocalDate.of(2026, 3, 13)
		);

		given(accountRepository.findAllByUser_Id(1)).willReturn(List.of());
		given(cardRepository.findAllByWithdrawalAccount_User_Id(1)).willReturn(List.of(card));
		given(dateRangeFactory.resolve(6, LocalDate.of(2026, 3, 10), false)).willReturn(window);
		given(cardTransactionOverrideRepository.findTopByCard_CardUniqueNoOrderByTransactionDateTimeDesc(9999L))
			.willReturn(Optional.of(latestLocal));
		given(cardTransactionApiClient.inquireTransactions(any())).willReturn(
			new CardTransactionDto.Response(
				"ISS-01",
				"테스트카드사",
				"생활카드",
				"1234-5678-9012-3456",
				0L,
				List.of(
					cardItem(5002L, "CAFE", "카페", 7L, "스타벅스", "20260312", "111500", 6200L, "true", "Y", "BILLED"),
					cardItem(5001L, "CAFE", "카페", 7L, "스타벅스", "20260310", "083000", 6100L, "true", "Y", "BILLED")
				)
			)
		);
		given(cardTransactionOverrideRepository.existsByCard_CardUniqueNoAndTransactionUniqueNo(9999L, 5002L)).willReturn(false);
		given(cardTransactionOverrideRepository.existsByCard_CardUniqueNoAndTransactionUniqueNo(9999L, 5001L)).willReturn(true);
		given(categoryRepository.getReferenceById(DEFAULT_CATEGORY_ID)).willReturn(defaultCategory);
		given(merchantRepository.findById(7)).willReturn(Optional.of(merchant));

		service.syncLatest(1, 6, false);

		ArgumentCaptor<CardTransactionDto.Request> requestCaptor = ArgumentCaptor.forClass(CardTransactionDto.Request.class);
		verify(cardTransactionApiClient).inquireTransactions(requestCaptor.capture());
		assertEquals("user-key-1", requestCaptor.getValue().userKey());
		assertEquals("1234-5678-9012-3456", requestCaptor.getValue().cardNo());
		assertEquals("123", requestCaptor.getValue().cvc());
		assertEquals("20260310", requestCaptor.getValue().startDate());
		assertEquals("20260313", requestCaptor.getValue().endDate());

		ArgumentCaptor<List<CardTransactionOverride>> saveCaptor = listCaptor();
		verify(cardTransactionOverrideRepository).saveAll(saveCaptor.capture());
		assertEquals(1, saveCaptor.getValue().size());
		CardTransactionOverride saved = saveCaptor.getValue().get(0);
		assertEquals(5002L, saved.getTransactionUniqueNo());
		assertEquals(merchant, saved.getMerchant());
		assertEquals(card, saved.getCard());
		assertEquals(defaultCategory, saved.getCategory());
	}

	private User user(Integer id, String userKey) {
		return User.builder()
			.id(id)
			.status(UserStatus.ACTIVE)
			.userKey(userKey)
			.nickname("tester")
			.email("tester@example.com")
			.build();
	}

	private Account account(User user, String id, String accountNo) {
		Account account = Account.create(user, accountNo, null, null, null, "001", "test-bank", null);
		ReflectionTestUtils.setField(account, "id", id);
		account.markVerified();
		return account;
	}

	private Card card(Account withdrawalAccount, Long cardUniqueNo, String cardNo, String cvc) {
		return Card.builder()
			.cardUniqueNo(cardUniqueNo)
			.withdrawalAccount(withdrawalAccount)
			.cvc(cvc)
			.cardNo(cardNo)
			.name("테스트카드")
			.issuerCode("ISS")
			.cardIssuerName("테스트카드사")
			.cardExpiryDate(LocalDateTime.of(2027, 12, 31, 0, 0))
			.cardDescription("기본 카드")
			.withdrawalDate("15")
			.baselinePerformance(0L)
			.maxBenefitLimit(0L)
			.build();
	}

	private DemandDepositTransactionDto.Item accountItem(
		Long transactionUniqueNo,
		String transactionDate,
		String transactionTime,
		String transactionType,
		String transactionTypeName,
		String transactionAccountNo,
		Long transactionBalance,
		Long transactionAfterBalance,
		String transactionSummary,
		String transactionMemo
	) {
		return new DemandDepositTransactionDto.Item(
			transactionUniqueNo,
			transactionDate,
			transactionTime,
			transactionType,
			transactionTypeName,
			transactionAccountNo,
			transactionBalance,
			transactionAfterBalance,
			transactionSummary,
			transactionMemo
		);
	}

	private CardTransactionDto.Item cardItem(
		Long transactionUniqueNo,
		String categoryId,
		String categoryName,
		Long merchantId,
		String merchantName,
		String transactionDate,
		String transactionTime,
		Long transactionBalance,
		String cardStatus,
		String billStatementsYn,
		String billStatementsStatus
	) {
		return new CardTransactionDto.Item(
			transactionUniqueNo,
			categoryId,
			categoryName,
			merchantId,
			merchantName,
			transactionDate,
			transactionTime,
			transactionBalance,
			cardStatus,
			billStatementsYn,
			billStatementsStatus
		);
	}

	private AccountTransactionOverride accountTransaction(
		Integer id,
		Category category,
		Account account,
		Long transactionUniqueNo,
		LocalDateTime transactionDateTime,
		Long transactionBalance,
		Long transactionAfterBalance,
		String transactionSummary
	) {
		return AccountTransactionOverride.builder()
			.id(id)
			.category(category)
			.transactionDateTime(transactionDateTime)
			.transactionBalance(transactionBalance)
			.transactionMemo(transactionSummary)
			.transactionUniqueNo(transactionUniqueNo)
			.transactionType(2)
			.transactionTypeName("출금")
			.transactionAfterBalance(transactionAfterBalance)
			.transactionSummary(transactionSummary)
			.account(account)
			.build();
	}

	private CardTransactionOverride cardTransaction(
		Integer id,
		Category category,
		Merchant merchant,
		Card card,
		Long transactionUniqueNo,
		LocalDateTime transactionDateTime,
		Long transactionBalance
	) {
		return CardTransactionOverride.builder()
			.id(id)
			.category(category)
			.transactionDateTime(transactionDateTime)
			.transactionBalance(transactionBalance)
			.transactionMemo(merchant.getName())
			.transactionUniqueNo(transactionUniqueNo)
			.merchant(merchant)
			.cardStatus(true)
			.billStatementsYn(true)
			.billStatementsStatus("BILLED")
			.card(card)
			.build();
	}

	@SuppressWarnings("unchecked")
	private <T> ArgumentCaptor<List<T>> listCaptor() {
		return (ArgumentCaptor<List<T>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
	}
}

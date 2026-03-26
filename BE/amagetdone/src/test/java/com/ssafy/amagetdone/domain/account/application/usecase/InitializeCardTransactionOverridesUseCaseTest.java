package com.ssafy.amagetdone.domain.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Card;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.entity.Merchant;
import com.ssafy.amagetdone.domain.account.model.repository.CardRepository;
import com.ssafy.amagetdone.domain.account.model.repository.MerchantRepository;
import com.ssafy.amagetdone.domain.account.support.util.CardRealTransactionScenario;
import com.ssafy.amagetdone.domain.account.support.util.CardRealTransactionScenario.CardSeed;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionCategoryAssignmentService;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.repository.CardTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisResultRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InitializeCardTransactionOverridesUseCaseTest {

	private static final Set<Integer> TRANSPORT_MERCHANT_IDS = Set.of(
		16695, 16696, 16697, 16689, 16690, 16691, 23586, 23678, 23482, 23130
	);
	private static final Set<Integer> FOOD_MERCHANT_IDS = Set.of(
		23040, 23241, 23338, 23404, 23056, 23087, 23053, 23126, 23151, 23118,
		23077, 23082, 23103, 23094, 23105, 23133, 23132, 23128, 23084, 23149
	);
	private static final Set<Integer> CONVENIENCE_MERCHANT_IDS = Set.of(
		24924, 25185, 26042, 23140, 23174, 23896, 23141, 23611, 24833, 16709
	);
	private static final Set<Integer> CAFE_MERCHANT_IDS = Set.of(
		16707, 23134, 23159, 23223, 23278, 23469, 23583, 16698, 23129, 23198
	);
	private static final Set<Integer> ONLINE_MERCHANT_IDS = Set.of(
		16706, 16704, 16705, 28603, 23054, 24482, 28203, 29414, 30383, 31556
	);
	private static final Set<Integer> SHOPPING_OR_LIFESTYLE_MERCHANT_IDS = Set.of(
		16692, 16693, 16694, 23101, 23123, 23097, 23108, 23164, 27873, 30139,
		23050, 23059, 23060, 23048, 23049, 23043, 23041, 23086, 23066, 23163
	);

	@Mock
	private CardRepository cardRepository;

	@Mock
	private MerchantRepository merchantRepository;

	@Mock
	private CardTransactionOverrideRepository cardTransactionOverrideRepository;

	@Mock
	private TransactionAnalysisResultRepository transactionAnalysisResultRepository;

	@Mock
	private TransactionCategoryAssignmentService transactionCategoryAssignmentService;

	@Test
	void executeDeletesLinkedAnalysisResultsBeforeReloadingCardTransactions() {
		Account withdrawalAccount = Account.create(
			User.builder().id(1).build(),
			"0012345678901234",
			"product-1",
			"Main account",
			"Demand deposit",
			"001",
			"Bank",
			"KRW"
		);
		Card card = Card.builder()
			.cardUniqueNo(123456789L)
			.withdrawalAccount(withdrawalAccount)
			.cvc("123")
			.cardNo("5241111122223333")
			.name("Card")
			.issuerCode("001")
			.cardIssuerName("Issuer")
			.cardExpiryDate(LocalDateTime.of(2029, 3, 31, 0, 0))
			.cardDescription("desc")
			.withdrawalDate("3")
			.baselinePerformance(0L)
			.maxBenefitLimit(0L)
			.build();
		Category category = Category.create(4, "etc");
		CardRealTransactionScenario scenario = new CardRealTransactionScenario();
		List<CardSeed> scenarioSeeds = scenario.completedMonthSeeds();
		Map<Integer, Merchant> merchantsById = scenarioSeeds.stream()
			.map(CardSeed::merchantId)
			.distinct()
			.collect(Collectors.toMap(
				Function.identity(),
				id -> Merchant.builder().id(id).name("merchant-" + id).build()
			));
		InitializeCardTransactionOverridesUseCase useCase = new InitializeCardTransactionOverridesUseCase(
			cardRepository,
			merchantRepository,
			cardTransactionOverrideRepository,
			transactionAnalysisResultRepository,
			scenario,
			transactionCategoryAssignmentService
		);
		CardTransactionOverride existing = CardTransactionOverride.builder().id(202).card(card).build();
		given(cardRepository.findByCardNo("5241111122223333")).willReturn(Optional.of(card));
		given(merchantRepository.findAllById(any())).willReturn(new ArrayList<>(merchantsById.values()));
		given(cardTransactionOverrideRepository.findAllByCard_CardUniqueNo(card.getCardUniqueNo())).willReturn(List.of(existing));
		given(transactionCategoryAssignmentService.assignCategories(any())).willAnswer(invocation -> {
			List<String> texts = invocation.getArgument(0);
			return texts.stream().collect(Collectors.toMap(Function.identity(), value -> category, (left, right) -> left));
		});

		AccountAdminDto.TransactionInitResponse response =
			useCase.execute(new AccountAdminDto.CardTransactionInitRequest("5241111122223333"));

		ArgumentCaptor<List<CardTransactionOverride>> captor = listCaptor();
		verify(transactionAnalysisResultRepository).deleteByTransaction_IdIn(List.of(202));
		verify(cardTransactionOverrideRepository).deleteByCard_CardUniqueNo(card.getCardUniqueNo());
		verify(transactionCategoryAssignmentService).assignCategories(any());
		verify(merchantRepository, never()).save(any(Merchant.class));
		verify(cardTransactionOverrideRepository).saveAll(captor.capture());
		ArgumentCaptor<Iterable<Integer>> merchantIdsCaptor = ArgumentCaptor.forClass(Iterable.class);
		verify(merchantRepository).findAllById(merchantIdsCaptor.capture());
		assertThat(toIdSet(merchantIdsCaptor.getValue())).isEqualTo(merchantsById.keySet());
		assertThat(response.targetType()).isEqualTo("card");
		assertThat(response.targetNo()).isEqualTo("5241111122223333");
		assertThat(response.transactionCount()).isEqualTo(captor.getValue().size());
		assertThat(captor.getValue().size()).isGreaterThanOrEqualTo(600);
		assertThat(captor.getValue())
			.extracting(CardTransactionOverride::getId)
			.containsOnlyNulls();
		assertThat(captor.getValue())
			.extracting(CardTransactionOverride::getTransactionDateTime)
			.map(YearMonth::from)
			.contains(YearMonth.of(2025, 12), YearMonth.of(2026, 1), YearMonth.of(2026, 2), YearMonth.of(2026, 3));
		assertThat(captor.getValue())
			.extracting(CardTransactionOverride::getTransactionDateTime)
			.map(dateTime -> dateTime.toLocalDate())
			.contains(LocalDate.of(2026, 3, 25));
		assertThat(countMatches(captor.getValue(), FOOD_MERCHANT_IDS)).isGreaterThanOrEqualTo(290);
		assertThat(countMatches(captor.getValue(), TRANSPORT_MERCHANT_IDS)).isGreaterThanOrEqualTo(160);
		assertThat(countMatches(captor.getValue(), CONVENIENCE_MERCHANT_IDS)).isGreaterThanOrEqualTo(110);
		assertThat(countMatches(captor.getValue(), CAFE_MERCHANT_IDS)).isGreaterThanOrEqualTo(55);
		assertThat(countMatches(captor.getValue(), SHOPPING_OR_LIFESTYLE_MERCHANT_IDS)).isGreaterThanOrEqualTo(40);
		assertThat(countMatches(captor.getValue(), ONLINE_MERCHANT_IDS)).isGreaterThanOrEqualTo(10);
		assertThat(countCanceledOrPendingTransactions(captor.getValue())).isGreaterThanOrEqualTo(8);
	}

	@Test
	void executeThrowsWhenSeededMerchantIsMissingFromDatabase() {
		CardRealTransactionScenario scenario = new CardRealTransactionScenario();
		List<CardSeed> scenarioSeeds = scenario.completedMonthSeeds();
		Integer missingMerchantId = scenarioSeeds.get(0).merchantId();
		Category category = Category.create(4, "etc");
		Map<Integer, Merchant> merchantsById = scenarioSeeds.stream()
			.map(CardSeed::merchantId)
			.distinct()
			.filter(id -> !id.equals(missingMerchantId))
			.collect(Collectors.toMap(
				Function.identity(),
				id -> Merchant.builder().id(id).name("merchant-" + id).build()
			));
		InitializeCardTransactionOverridesUseCase useCase = new InitializeCardTransactionOverridesUseCase(
			cardRepository,
			merchantRepository,
			cardTransactionOverrideRepository,
			transactionAnalysisResultRepository,
			scenario,
			transactionCategoryAssignmentService
		);
		Card card = Card.builder()
			.cardUniqueNo(123456789L)
			.cardNo("5241111122223333")
			.build();
		given(cardRepository.findByCardNo("5241111122223333")).willReturn(Optional.of(card));
		assertThatThrownBy(() -> useCase.execute(new AccountAdminDto.CardTransactionInitRequest("5241111122223333")))
			.isInstanceOf(AccountException.class)
			.extracting(exception -> ((AccountException)exception).getErrorCode())
			.isEqualTo(AccountErrorCode.ACCOUNT_ERROR);
	}

	@Test
	void executeThrowsWhenCardDoesNotExist() {
		InitializeCardTransactionOverridesUseCase useCase = new InitializeCardTransactionOverridesUseCase(
			cardRepository,
			merchantRepository,
			cardTransactionOverrideRepository,
			transactionAnalysisResultRepository,
			new CardRealTransactionScenario(),
			transactionCategoryAssignmentService
		);
		given(cardRepository.findByCardNo("missing")).willReturn(Optional.empty());

		assertThatThrownBy(() -> useCase.execute(new AccountAdminDto.CardTransactionInitRequest("missing")))
			.isInstanceOf(AccountException.class)
			.extracting(exception -> ((AccountException)exception).getErrorCode())
			.isEqualTo(AccountErrorCode.CARD_NOT_FOUND);
	}

	@SuppressWarnings("unchecked")
	private <T> ArgumentCaptor<List<T>> listCaptor() {
		return (ArgumentCaptor<List<T>>) (ArgumentCaptor<?>)ArgumentCaptor.forClass(List.class);
	}

	private long countMatches(List<CardTransactionOverride> transactions, Set<Integer> merchantIds) {
		return transactions.stream()
			.map(transaction -> transaction.getMerchant().getId())
			.filter(merchantIds::contains)
			.count();
	}

	private long countCanceledOrPendingTransactions(List<CardTransactionOverride> transactions) {
		return transactions.stream()
			.filter(transaction -> !transaction.isCardStatus() || !transaction.isBillStatementsYn())
			.count();
	}

	private Set<Integer> toIdSet(Iterable<Integer> merchantIds) {
		Set<Integer> ids = new java.util.HashSet<>();
		for (Integer merchantId : merchantIds) {
			ids.add(merchantId);
		}
		return ids;
	}
}

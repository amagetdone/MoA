package com.ssafy.amagetdone.domain.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
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
		InitializeCardTransactionOverridesUseCase useCase = new InitializeCardTransactionOverridesUseCase(
			cardRepository,
			merchantRepository,
			cardTransactionOverrideRepository,
			transactionAnalysisResultRepository,
			new CardRealTransactionScenario(),
			transactionCategoryAssignmentService
		);
		CardTransactionOverride existing = CardTransactionOverride.builder().id(202).card(card).build();
		given(cardRepository.findByCardNo("5241111122223333")).willReturn(Optional.of(card));
		given(merchantRepository.findById(any())).willReturn(Optional.empty());
		given(merchantRepository.save(any(Merchant.class))).willAnswer(invocation -> invocation.getArgument(0));
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
		verify(cardTransactionOverrideRepository).saveAll(captor.capture());
		assertThat(response.targetType()).isEqualTo("card");
		assertThat(response.targetNo()).isEqualTo("5241111122223333");
		assertThat(response.transactionCount()).isEqualTo(captor.getValue().size());
		assertThat(captor.getValue().size()).isGreaterThanOrEqualTo(80);
		assertThat(captor.getValue())
			.extracting(CardTransactionOverride::getTransactionDateTime)
			.map(YearMonth::from)
			.contains(YearMonth.of(2025, 12), YearMonth.of(2026, 1), YearMonth.of(2026, 2), YearMonth.of(2026, 3));
		assertThat(captor.getValue())
			.extracting(CardTransactionOverride::getTransactionDateTime)
			.map(dateTime -> dateTime.toLocalDate())
			.contains(LocalDate.of(2026, 3, 20));
		assertThat(countFoodMerchants(captor.getValue())).isGreaterThanOrEqualTo(35);
		assertThat(countTransportMerchants(captor.getValue())).isGreaterThanOrEqualTo(15);
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

	private long countFoodMerchants(List<CardTransactionOverride> transactions) {
		return transactions.stream()
			.map(transaction -> transaction.getMerchant().getName())
			.filter(name -> name.contains("버거")
				|| name.contains("김밥")
				|| name.contains("맘스터치")
				|| name.contains("한솥")
				|| name.contains("국밥")
				|| name.contains("마라")
				|| name.contains("우동")
				|| name.contains("덮밥")
				|| name.contains("카페")
				|| name.contains("치킨")
				|| name.contains("토스트")
				|| name.contains("분식"))
			.count();
	}

	private long countTransportMerchants(List<CardTransactionOverride> transactions) {
		return transactions.stream()
			.map(transaction -> transaction.getMerchant().getName())
			.filter(name -> name.contains("택시")
				|| name.contains("지하철")
				|| name.contains("버스")
				|| name.contains("코레일")
				|| name.contains("유페이"))
			.count();
	}
}

package com.ssafy.amagetdone.domain.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.infra.client.SsafyCreditCardQueryApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.dto.CreditCardQueryDto;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Card;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.account.model.repository.CardRepository;
import com.ssafy.amagetdone.domain.account.support.util.LocalCardUniqueNoMapper;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncLinkedCardsUseCaseTest {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private CardRepository cardRepository;

	@Mock
	private SsafyCreditCardQueryApiClient ssafyCreditCardQueryApiClient;

	@Mock
	private LocalCardUniqueNoMapper localCardUniqueNoMapper;

	@InjectMocks
	private SyncLinkedCardsUseCase syncLinkedCardsUseCase;

	@Test
	void executeReturnsNormallyWhenSsafyCardListIsEmpty() {
		Account linkedAccount = linkedAccount("0011234567890123");
		when(accountRepository.findActiveLinkedAccountByUserIdAndAccountNo(7, "0011234567890123"))
			.thenReturn(Optional.of(linkedAccount));
		when(ssafyCreditCardQueryApiClient.inquireLinkedCards(new CreditCardQueryDto.Request("user-key")))
			.thenReturn(new CreditCardQueryDto.Response(List.of()));

		syncLinkedCardsUseCase.execute(7, "user-key", "0011234567890123");

		verify(cardRepository, never()).save(any(Card.class));
	}

	@Test
	void executeUpsertsOnlyCardsLinkedToRequestedAccount() {
		Account linkedAccount = linkedAccount("0011234567890123");
		when(accountRepository.findActiveLinkedAccountByUserIdAndAccountNo(7, "0011234567890123"))
			.thenReturn(Optional.of(linkedAccount));
		when(ssafyCreditCardQueryApiClient.inquireLinkedCards(new CreditCardQueryDto.Request("user-key")))
			.thenReturn(new CreditCardQueryDto.Response(List.of(
				cardItem("CARD-001", "0011234567890123", "1111222233334444", "123"),
				cardItem("CARD-OTHER", "0099999999999999", "9999888877776666", "999")
			)));
		when(localCardUniqueNoMapper.toLocalId("CARD-001")).thenReturn(101L);
		when(cardRepository.findById(101L)).thenReturn(Optional.empty());
		when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

		syncLinkedCardsUseCase.execute(7, "user-key", "0011234567890123");

		ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
		verify(cardRepository).save(captor.capture());
		Card saved = captor.getValue();
		assertThat(saved.getCardUniqueNo()).isEqualTo(101L);
		assertThat(saved.getWithdrawalAccount()).isEqualTo(linkedAccount);
		assertThat(saved.getCardNo()).isEqualTo("1111222233334444");
		assertThat(saved.getCvc()).isEqualTo("123");
		assertThat(saved.getName()).isEqualTo("테스트 카드");
		assertThat(saved.getIssuerCode()).isEqualTo("1005");
		assertThat(saved.getCardIssuerName()).isEqualTo("신한카드");
		assertThat(saved.getBaselinePerformance()).isEqualTo(0L);
		assertThat(saved.getMaxBenefitLimit()).isEqualTo(200000L);
	}

	@Test
	void executeUpdatesExistingCardInPlace() {
		Account linkedAccount = linkedAccount("0011234567890123");
		Card existingCard = Card.builder()
			.cardUniqueNo(101L)
			.withdrawalAccount(linkedAccount)
			.cvc("111")
			.cardNo("1111222233334444")
			.name("예전 카드명")
			.issuerCode("1001")
			.cardIssuerName("예전 카드사")
			.cardExpiryDate(LocalDateTime.of(2027, 1, 1, 0, 0))
			.cardDescription("old")
			.withdrawalDate("10")
			.baselinePerformance(10L)
			.maxBenefitLimit(20L)
			.build();

		when(accountRepository.findActiveLinkedAccountByUserIdAndAccountNo(7, "0011234567890123"))
			.thenReturn(Optional.of(linkedAccount));
		when(ssafyCreditCardQueryApiClient.inquireLinkedCards(new CreditCardQueryDto.Request("user-key")))
			.thenReturn(new CreditCardQueryDto.Response(List.of(cardItem("CARD-001", "0011234567890123", "5555666677778888", "456"))));
		when(localCardUniqueNoMapper.toLocalId("CARD-001")).thenReturn(101L);
		when(cardRepository.findById(101L)).thenReturn(Optional.of(existingCard));
		when(cardRepository.save(existingCard)).thenReturn(existingCard);

		syncLinkedCardsUseCase.execute(7, "user-key", "0011234567890123");

		assertThat(existingCard.getWithdrawalAccount()).isEqualTo(linkedAccount);
		assertThat(existingCard.getCardNo()).isEqualTo("5555666677778888");
		assertThat(existingCard.getCvc()).isEqualTo("456");
		assertThat(existingCard.getName()).isEqualTo("테스트 카드");
		assertThat(existingCard.getIssuerCode()).isEqualTo("1005");
		assertThat(existingCard.getCardIssuerName()).isEqualTo("신한카드");
		assertThat(existingCard.getWithdrawalDate()).isEqualTo("4");
		assertThat(existingCard.getBaselinePerformance()).isEqualTo(0L);
		assertThat(existingCard.getMaxBenefitLimit()).isEqualTo(200000L);
		verify(cardRepository).save(existingCard);
	}

	@Test
	void executeThrowsWhenCardsExistButNoneMatchLinkedAccount() {
		Account linkedAccount = linkedAccount("0011234567890123");
		when(accountRepository.findActiveLinkedAccountByUserIdAndAccountNo(7, "0011234567890123"))
			.thenReturn(Optional.of(linkedAccount));
		when(ssafyCreditCardQueryApiClient.inquireLinkedCards(new CreditCardQueryDto.Request("user-key")))
			.thenReturn(new CreditCardQueryDto.Response(List.of(cardItem("CARD-001", "0099999999999999", "1111222233334444", "123"))));

		assertThatThrownBy(() -> syncLinkedCardsUseCase.execute(7, "user-key", "0011234567890123"))
			.isInstanceOf(AccountException.class)
			.extracting("errorCode")
			.isEqualTo(AccountErrorCode.LINKED_ACCOUNT_CARD_NOT_FOUND);
	}

	private Account linkedAccount(String accountNo) {
		User user = User.builder()
			.id(7)
			.userKey("user-key")
			.email("tester@example.com")
			.nickname("tester")
			.build();
		Account account = Account.create(user, accountNo, null, "주계좌", "수시입출금", "001", "싸피은행", "KRW");
		account.markVerified();
		account.markActive();
		return account;
	}

	private CreditCardQueryDto.CardItem cardItem(
		String cardUniqueNo,
		String withdrawalAccountNo,
		String cardNo,
		String cvc
	) {
		return new CreditCardQueryDto.CardItem(
			cardNo,
			cvc,
			cardUniqueNo,
			"1005",
			"신한카드",
			"테스트 카드",
			0L,
			200000L,
			"생활 할인",
			"20290409",
			withdrawalAccountNo,
			"4"
		);
	}
}

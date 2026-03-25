package com.ssafy.amagetdone.domain.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.infra.client.AccountSsafyApiProperties;
import com.ssafy.amagetdone.domain.account.infra.client.SsafyAdminCreditCardApiClient;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Card;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.entity.Merchant;
import com.ssafy.amagetdone.domain.account.model.repository.CardRepository;
import com.ssafy.amagetdone.domain.account.model.repository.MerchantRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.notification.application.event.TransactionSyncedEvent;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionCategoryAssignmentService;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.CardTransactionDto;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.repository.CardTransactionOverrideRepository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class ExecuteAdminCardPaymentUseCaseTest {

	@Test
	void executePersistsCardOverrideAfterPayment() {
		CardRepository cardRepository = mock(CardRepository.class);
		CardTransactionOverrideRepository cardTransactionOverrideRepository = mock(CardTransactionOverrideRepository.class);
		MerchantRepository merchantRepository = mock(MerchantRepository.class);
		SsafyAdminCreditCardApiClient ssafyAdminCreditCardApiClient = mock(SsafyAdminCreditCardApiClient.class);
		TransactionCategoryAssignmentService transactionCategoryAssignmentService = mock(TransactionCategoryAssignmentService.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		AccountSsafyApiProperties properties = new AccountSsafyApiProperties();
		properties.setApiKey("test-api-key");
		ExecuteAdminCardPaymentUseCase useCase = new ExecuteAdminCardPaymentUseCase(
			cardRepository,
			cardTransactionOverrideRepository,
			merchantRepository,
			ssafyAdminCreditCardApiClient,
			transactionCategoryAssignmentService,
			properties,
			applicationEventPublisher
		);

		User user = User.createPendingSocialUser();
		user.setId(7);
		user.setUserKey("user-key");
		user.setBirthDate(LocalDate.of(2000, 1, 1));
		Account account = Account.create(
			user,
			"0011234567890123",
			"product-1",
			"생활비 계좌",
			"수시입출금",
			"001",
			"테스트은행",
			"KRW"
		);
		account.markVerifiedLinked();
		Card card = Card.create(
			777L,
			account,
			"321",
			"5241111122223333",
			"테스트카드",
			"1005",
			"테스트카드사",
			LocalDateTime.of(2029, 12, 31, 0, 0),
			"설명",
			"1",
			10L,
			100000L
		);
		AccountAdminDto.CardPaymentRequest request = new AccountAdminDto.CardPaymentRequest(
			"5241111122223333",
			101001L,
			28900L
		);
		CardTransactionDto.Item item = new CardTransactionDto.Item(
			88L,
			"CG-1",
			"생활",
			101001L,
			"테스트가맹점",
			"20260324",
			"092700",
			28900L,
			"Y",
			"Y",
			"BILLED"
		);
		Category category = Category.create(4, "기타");
		Merchant merchant = Merchant.builder().id(101001).name("테스트가맹점").build();

		when(cardRepository.findByCardNo("5241111122223333")).thenReturn(Optional.of(card));
		when(ssafyAdminCreditCardApiClient.createCardTransaction(
			"test-api-key",
			"user-key",
			"5241111122223333",
			"321",
			101001L,
			28900L
		)).thenReturn(item);
		when(transactionCategoryAssignmentService.assignCategories(java.util.List.of("테스트가맹점")))
			.thenReturn(Map.of("테스트가맹점", category));
		when(merchantRepository.findById(101001)).thenReturn(Optional.of(merchant));

		AccountAdminDto.PaymentResponse result = useCase.execute(7, request);

		ArgumentCaptor<CardTransactionOverride> captor = ArgumentCaptor.forClass(CardTransactionOverride.class);
		ArgumentCaptor<TransactionSyncedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionSyncedEvent.class);
		verify(cardTransactionOverrideRepository).save(captor.capture());
		verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
		assertThat(captor.getValue().getCard()).isEqualTo(card);
		assertThat(captor.getValue().getMerchant()).isEqualTo(merchant);
		assertThat(captor.getValue().getTransactionUniqueNo()).isEqualTo(88L);
		assertThat(eventCaptor.getValue().userId()).isEqualTo(7);
		assertThat(eventCaptor.getValue().sourceType()).isEqualTo("CARD");
		assertThat(eventCaptor.getValue().transactionId()).isEqualTo("88");
		assertThat(eventCaptor.getValue().amount()).isEqualTo(28900L);
		assertThat(eventCaptor.getValue().merchantOrMemo()).isEqualTo(merchant.getName());
		assertThat(eventCaptor.getValue().occurredAt()).isEqualTo(LocalDateTime.of(2026, 3, 24, 9, 27));
		assertThat(result.targetType()).isEqualTo("card");
		assertThat(result.targetNo()).isEqualTo("5241111122223333");
		assertThat(result.status()).isEqualTo("BILLED");
	}
}

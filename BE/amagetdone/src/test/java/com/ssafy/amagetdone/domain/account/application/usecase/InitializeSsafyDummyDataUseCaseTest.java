package com.ssafy.amagetdone.domain.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.infra.client.SsafyAdminCreditCardApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.SsafyAdminDemandDepositApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.dto.AdminCreditCardDto;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Card;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.entity.Merchant;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.account.model.repository.CardRepository;
import com.ssafy.amagetdone.domain.account.model.repository.CategoryRepository;
import com.ssafy.amagetdone.domain.account.model.repository.MerchantRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.repository.AccountTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.CardTransactionOverrideRepository;
import com.ssafy.amagetdone.global.infra.ssafy.exception.SsafyApiException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InitializeSsafyDummyDataUseCaseTest {

	private static final String DEFAULT_CATEGORY_NAME = "\uAE30\uD0C0\uC11C\uBE44\uC2A4";

	@Mock
	private FindAccountUserByUserKeyUseCase findAccountUserByUserKeyUseCase;

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
	private SsafyAdminDemandDepositApiClient ssafyAdminDemandDepositApiClient;

	@Mock
	private SsafyAdminCreditCardApiClient ssafyAdminCreditCardApiClient;

	@Test
	void executeCreatesVariedTransactionsAcrossPreviousThreeCompletedMonths() {
		InitializeSsafyDummyDataUseCase useCase = useCaseWithClock("2026-03-17T00:00:00Z");
		AccountAdminDto.DataInitRequest request = new AccountAdminDto.DataInitRequest("admin-key", "user-key");
		User user = User.builder().id(7).userKey("user-key").build();

		mockCommonSuccess(user);
		given(categoryRepository.findById(4)).willReturn(Optional.of(Category.create(4, DEFAULT_CATEGORY_NAME)));
		given(merchantRepository.findById(any())).willReturn(Optional.empty());
		given(merchantRepository.save(any(Merchant.class))).willAnswer(invocation -> invocation.getArgument(0));

		AccountAdminDto.DataInitResponse response = useCase.execute(request);

		assertThat(response.accountNo()).isEqualTo("1234567890123456");
		assertThat(response.cardNo()).isEqualTo("1003622654847049");
		assertThat(response.cardUniqueNo()).isEqualTo("1001-eebb34baf8204cf");
		assertThat(response.accountTransactionCount()).isEqualTo(16);
		assertThat(response.cardTransactionCount()).isEqualTo(28);

		ArgumentCaptor<List<AccountTransactionOverride>> accountCaptor = listCaptor();
		ArgumentCaptor<List<CardTransactionOverride>> cardCaptor = listCaptor();
		verify(accountTransactionOverrideRepository).saveAll(accountCaptor.capture());
		verify(cardTransactionOverrideRepository).saveAll(cardCaptor.capture());

		List<AccountTransactionOverride> accountTransactions = accountCaptor.getValue();
		List<CardTransactionOverride> cardTransactions = cardCaptor.getValue();
		assertThat(accountTransactions).hasSize(16);
		assertThat(cardTransactions).hasSize(28);
		assertThat(accountTransactions)
			.extracting(AccountTransactionOverride::getTransactionDateTime)
			.allMatch(this::isWithinPreviousThreeCompletedMonths);
		assertThat(cardTransactions)
			.extracting(CardTransactionOverride::getTransactionDateTime)
			.allMatch(this::isWithinPreviousThreeCompletedMonths);
		assertThat(accountTransactions.stream()
			.map(transaction -> YearMonth.from(transaction.getTransactionDateTime()))
			.collect(Collectors.toSet()))
			.containsExactlyInAnyOrder(YearMonth.of(2025, 12), YearMonth.of(2026, 1), YearMonth.of(2026, 2));
		assertThat(cardTransactions.stream()
			.map(transaction -> YearMonth.from(transaction.getTransactionDateTime()))
			.collect(Collectors.toSet()))
			.containsExactlyInAnyOrder(YearMonth.of(2025, 12), YearMonth.of(2026, 1), YearMonth.of(2026, 2));
		assertThat(accountTransactions)
			.extracting(transaction -> transaction.getCategory().getName())
			.containsOnly(DEFAULT_CATEGORY_NAME);
		assertThat(cardTransactions)
			.extracting(transaction -> transaction.getCategory().getName())
			.containsOnly(DEFAULT_CATEGORY_NAME);
		assertThat(accountTransactions)
			.extracting(AccountTransactionOverride::getTransactionSummary)
			.contains("\uC6D4\uAE09", "\uC6D4\uC138", "\uD734\uB300\uD3F0 \uC694\uAE08", "\uC2E4\uC190\uBCF4\uD5D8", "\uBE44\uC0C1\uAE08 \uC774\uCCB4");
		assertThat(cardTransactions)
			.extracting(CardTransactionOverride::getTransactionMemo)
			.contains(
				"\uB137\uD50C\uB9AD\uC2A4",
				"\uC2A4\uD0C0\uBC85\uC2A4 \uC11C\uC6B8\uC5ED\uC810",
				"\uC774\uB9C8\uD2B8 \uC740\uD3C9\uC810",
				"\uCE74\uCE74\uC624\uD0DD\uC2DC",
				"\uC62C\uB9AC\uBE0C\uC601 \uAC15\uB0A8\uC810",
				"\uCFE0\uD321",
				"\uBC30\uB2EC\uC758\uBBFC\uC871",
				"\uC11C\uC6B8\uC544\uC0B0\uBCD1\uC6D0"
			);
	}

	@Test
	void executeStoresDefaultCategoryWithoutCallingInferenceApi() {
		InitializeSsafyDummyDataUseCase useCase = useCaseWithClock("2026-03-17T00:00:00Z");
		User user = User.builder().id(7).userKey("user-key").build();
		Category defaultCategory = Category.create(4, DEFAULT_CATEGORY_NAME);

		mockCommonSuccess(user);
		given(categoryRepository.findById(4)).willReturn(Optional.of(defaultCategory));
		given(merchantRepository.findById(any())).willReturn(Optional.empty());
		given(merchantRepository.save(any(Merchant.class))).willAnswer(invocation -> invocation.getArgument(0));

		useCase.execute(new AccountAdminDto.DataInitRequest("admin-key", "user-key"));

		ArgumentCaptor<List<AccountTransactionOverride>> accountCaptor = listCaptor();
		ArgumentCaptor<List<CardTransactionOverride>> cardCaptor = listCaptor();
		verify(accountTransactionOverrideRepository).saveAll(accountCaptor.capture());
		verify(cardTransactionOverrideRepository).saveAll(cardCaptor.capture());

		assertThat(accountCaptor.getValue())
			.extracting(transaction -> transaction.getCategory().getName())
			.containsOnly(DEFAULT_CATEGORY_NAME);
		assertThat(cardCaptor.getValue())
			.extracting(transaction -> transaction.getCategory().getName())
			.containsOnly(DEFAULT_CATEGORY_NAME);
	}

	@Test
	void executeThrowsBadRequestWhenApiKeyIsBlank() {
		InitializeSsafyDummyDataUseCase useCase = useCaseWithClock("2026-03-17T00:00:00Z");

		assertThatThrownBy(() -> useCase.execute(new AccountAdminDto.DataInitRequest(" ", "user-key")))
			.isInstanceOf(AccountException.class)
			.extracting(exception -> ((AccountException)exception).getErrorCode())
			.isEqualTo(AccountErrorCode.INVALID_DATA_INIT_REQUEST);
	}

	@Test
	void executePropagatesSsafyErrorDetailWhenAccountCreationFails() {
		InitializeSsafyDummyDataUseCase useCase = useCaseWithClock("2026-03-17T00:00:00Z");
		User user = User.builder().id(7).userKey("user-key").build();
		given(findAccountUserByUserKeyUseCase.execute("user-key")).willReturn(user);
		given(categoryRepository.findById(4)).willReturn(Optional.of(Category.create(4, DEFAULT_CATEGORY_NAME)));
		given(ssafyAdminDemandDepositApiClient.createAccount("admin-key", "user-key", "001-1-ee26d1d80a4542"))
			.willThrow(new AccountException(
				AccountErrorCode.DEMAND_DEPOSIT_ACCOUNT_API_CALL_FAILED,
				new SsafyApiException(
					SsafyApiException.Reason.FAILED_RESPONSE_CODE,
					"H1008",
					"API_KEY\uAC00 \uC720\uD6A8\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."
				)
			));

		assertThatThrownBy(() -> useCase.execute(new AccountAdminDto.DataInitRequest("admin-key", "user-key")))
			.isInstanceOf(AccountException.class)
			.satisfies(exception -> {
				AccountException accountException = (AccountException)exception;
				assertThat(accountException.getErrorCode()).isEqualTo(AccountErrorCode.DEMAND_DEPOSIT_ACCOUNT_API_CALL_FAILED);
				assertThat(accountException.getDetail()).containsEntry("responseCode", "H1008");
			});
	}

	private InitializeSsafyDummyDataUseCase useCaseWithClock(String instant) {
		InitializeSsafyDummyDataUseCase useCase = new InitializeSsafyDummyDataUseCase(
			findAccountUserByUserKeyUseCase,
			accountRepository,
			cardRepository,
			categoryRepository,
			merchantRepository,
			accountTransactionOverrideRepository,
			cardTransactionOverrideRepository,
			ssafyAdminDemandDepositApiClient,
			ssafyAdminCreditCardApiClient
		);
		ReflectionTestUtils.setField(useCase, "clock", Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Seoul")));
		return useCase;
	}

	private void mockCommonSuccess(User user) {
		given(findAccountUserByUserKeyUseCase.execute("user-key")).willReturn(user);
		given(accountRepository.findByUserIdAndAccountNo(user.getId(), "1234567890123456")).willReturn(Optional.empty());
		given(accountRepository.findByUserIdAndAccountNo(user.getId(), "9999999999999999")).willReturn(Optional.empty());
		given(ssafyAdminDemandDepositApiClient.createAccount("admin-key", "user-key", "001-1-ee26d1d80a4542"))
			.willReturn(new SsafyAdminDemandDepositApiClient.AccountCreationResult("001", "1234567890123456", "KRW", "\uC6D0\uD654"));
		given(ssafyAdminDemandDepositApiClient.createHelperAccount("admin-key", "user-key", "001-1-ee26d1d80a4542"))
			.willReturn(new SsafyAdminDemandDepositApiClient.AccountCreationResult("001", "9999999999999999", "KRW", "\uC6D0\uD654"));
		given(ssafyAdminCreditCardApiClient.createCard("admin-key", "user-key", "1001-eebb34baf8204cf", "1234567890123456", "3"))
			.willReturn(new AdminCreditCardDto.CreatedCard(
				"1003622654847049",
				"713",
				"1001-eebb34baf8204cf",
				"1001",
				"KB\uAD6D\uBBFC\uCE74\uB4DC",
				"SSAFY \uC2A4\uB9C8\uC77C\uCE74\uB4DC",
				500000L,
				10000L,
				"\uC0DD\uD65C 10% \uD560\uC778",
				"20290317",
				"1234567890123456",
				"3"
			));
		given(accountRepository.save(any(Account.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(cardRepository.save(any(Card.class))).willAnswer(invocation -> invocation.getArgument(0));
	}

	private boolean isWithinPreviousThreeCompletedMonths(LocalDateTime dateTime) {
		return !dateTime.isBefore(LocalDateTime.of(2025, 12, 1, 0, 0))
			&& !dateTime.isAfter(LocalDateTime.of(2026, 2, 28, 23, 59, 59));
	}

	@SuppressWarnings("unchecked")
	private <T> ArgumentCaptor<List<T>> listCaptor() {
		return (ArgumentCaptor<List<T>>) (ArgumentCaptor<?>)ArgumentCaptor.forClass(List.class);
	}
}

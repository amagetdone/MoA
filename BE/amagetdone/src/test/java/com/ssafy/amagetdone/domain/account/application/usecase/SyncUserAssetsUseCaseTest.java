package com.ssafy.amagetdone.domain.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.infra.client.SsafyDemandDepositAccountApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositAccountDto;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncUserAssetsUseCaseTest {

	@Mock
	private FindAccountUserUseCase findAccountUserUseCase;

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private SsafyDemandDepositAccountApiClient ssafyDemandDepositAccountApiClient;

	@Mock
	private SyncLinkedCardsUseCase syncLinkedCardsUseCase;

	@InjectMocks
	private SyncUserAssetsUseCase syncUserAssetsUseCase;

	@Test
	void upsertsSsafyAccountsAndSyncsCardsForGeneralUser() {
		User user = User.createPendingSocialUser();
		user.updateUserKey("user-key");

		when(findAccountUserUseCase.execute(7)).thenReturn(user);
		when(accountRepository.findAllByUserId(7)).thenReturn(List.of());
		when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(ssafyDemandDepositAccountApiClient.inquireAccounts(any())).thenReturn(
			new DemandDepositAccountDto.AccountListResponse(List.of(
				new DemandDepositAccountDto.AccountItem(
					"001",
					"bank-a",
					"user",
					"0011234567890123",
					"main-account",
					"1",
					"demand-deposit",
					"20260320",
					"20360320",
					100000L,
					100000L,
					100000L,
					"KRW"
				),
				new DemandDepositAccountDto.AccountItem(
					"002",
					"bank-b",
					"user",
					"0021234567890123",
					"sub-account",
					"1",
					"demand-deposit",
					"20260320",
					"20360320",
					200000L,
					200000L,
					200000L,
					"KRW"
				)
			))
		);

		syncUserAssetsUseCase.execute(7);

		verify(accountRepository, times(2)).save(any(Account.class));
		verify(syncLinkedCardsUseCase).execute(7, "user-key", "0011234567890123");
		verify(syncLinkedCardsUseCase).execute(7, "user-key", "0021234567890123");
	}

	@Test
	void throwsWhenUserKeyIsBlank() {
		User user = User.createPendingSocialUser();
		user.updateUserKey(" ");

		when(findAccountUserUseCase.execute(7)).thenReturn(user);

		assertThatThrownBy(() -> syncUserAssetsUseCase.execute(7))
			.isInstanceOf(AccountException.class)
			.extracting("errorCode")
			.isEqualTo(AccountErrorCode.USER_KEY_REQUIRED);
	}

	@Test
	void continuesWhenSingleCardSyncFails() {
		User user = User.createPendingSocialUser();
		user.updateUserKey("user-key");

		when(findAccountUserUseCase.execute(7)).thenReturn(user);
		when(accountRepository.findAllByUserId(7)).thenReturn(List.of());
		when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(ssafyDemandDepositAccountApiClient.inquireAccounts(any())).thenReturn(
			new DemandDepositAccountDto.AccountListResponse(List.of(
				new DemandDepositAccountDto.AccountItem(
					"001",
					"bank-a",
					"user",
					"0011234567890123",
					"main-account",
					"1",
					"demand-deposit",
					"20260320",
					"20360320",
					100000L,
					100000L,
					100000L,
					"KRW"
				),
				new DemandDepositAccountDto.AccountItem(
					"002",
					"bank-b",
					"user",
					"0021234567890123",
					"sub-account",
					"1",
					"demand-deposit",
					"20260320",
					"20360320",
					200000L,
					200000L,
					200000L,
					"KRW"
				)
			))
		);
		doThrow(new AccountException(AccountErrorCode.LINKED_CARD_SYNC_FAILED))
			.when(syncLinkedCardsUseCase).execute(7, "user-key", "0011234567890123");

		syncUserAssetsUseCase.execute(7);

		verify(syncLinkedCardsUseCase).execute(7, "user-key", "0011234567890123");
		verify(syncLinkedCardsUseCase).execute(7, "user-key", "0021234567890123");
	}
}

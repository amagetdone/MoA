package com.ssafy.amagetdone.domain.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

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
class ReconcileLinkedAccountsUseCaseTest {

	@Mock
	private FindAccountUserUseCase findAccountUserUseCase;

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private SsafyDemandDepositAccountApiClient ssafyDemandDepositAccountApiClient;

	@InjectMocks
	private ReconcileLinkedAccountsUseCase reconcileLinkedAccountsUseCase;

	@Test
	void inactivatesMissingLinkedAccountsAndReturnsOnlyActiveOnes() {
			User user = mock(User.class);
			when(user.getUserKey()).thenReturn("user-key");
			when(findAccountUserUseCase.execute(7)).thenReturn(user);

		Account activeLinkedAccount = Account.create(
			user,
			"0011234567890123",
			null,
			"생활비 통장",
			"수시입출금",
			"001",
			"한국은행",
			"KRW"
		);
		activeLinkedAccount.markVerified();

		Account missingLinkedAccount = Account.create(
			user,
			"0019999999999999",
			null,
			"과거 통장",
			"수시입출금",
			"001",
			"한국은행",
			"KRW"
		);
		missingLinkedAccount.markVerified();

		when(accountRepository.findAllLinkedAccountsByUserId(7)).thenReturn(List.of(activeLinkedAccount, missingLinkedAccount));
		when(ssafyDemandDepositAccountApiClient.inquireAccounts(any())).thenReturn(
			new DemandDepositAccountDto.AccountListResponse(List.of(
				new DemandDepositAccountDto.AccountItem(
					"001",
					"한국은행",
					"테스터",
					"0011234567890123",
					"생활비 통장",
					"1",
					"수시입출금",
					"20260318",
					"20300318",
					100000000L,
					50000000L,
					250000L,
					"KRW"
				)
			))
		);

		List<Account> result = reconcileLinkedAccountsUseCase.execute(7);

		assertThat(result).containsExactly(activeLinkedAccount);
		assertThat(missingLinkedAccount.isActive()).isFalse();
		verify(accountRepository).save(missingLinkedAccount);
	}

	@Test
	void returnsEmptyListWithoutUserKeyLookupWhenNoLinkedAccountsExist() {
		when(accountRepository.findAllLinkedAccountsByUserId(7)).thenReturn(List.of());

		List<Account> result = reconcileLinkedAccountsUseCase.execute(7);

		assertThat(result).isEmpty();
		verifyNoInteractions(findAccountUserUseCase);
		verifyNoInteractions(ssafyDemandDepositAccountApiClient);
	}
}

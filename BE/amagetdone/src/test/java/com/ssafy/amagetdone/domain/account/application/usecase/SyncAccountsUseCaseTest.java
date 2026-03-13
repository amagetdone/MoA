package com.ssafy.amagetdone.domain.account.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.account.infra.client.SsafyDemandDepositAccountApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositAccountDto;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncAccountsUseCaseTest {

	@Mock
	private FindAccountUserUseCase findAccountUserUseCase;

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private SsafyDemandDepositAccountApiClient accountApiClient;

	@InjectMocks
	private SyncAccountsUseCase syncAccountsUseCase;

	@Test
	void syncsOpenApiAccountsIntoLocalRepository() {
		User user = mock(User.class);
		when(user.getId()).thenReturn(7);
		when(user.getUserKey()).thenReturn("user-key-123");
		when(findAccountUserUseCase.execute(7)).thenReturn(user);
		when(accountRepository.findByUserIdAndAccountNo(7, "0011234567890123")).thenReturn(Optional.empty());
		when(accountApiClient.inquireAccounts(new DemandDepositAccountDto.AccountListRequest("user-key-123")))
			.thenReturn(
				new DemandDepositAccountDto.AccountListResponse(
					List.of(
						new DemandDepositAccountDto.AccountItem(
							"001",
							"한국은행",
							"USER",
							"0011234567890123",
							"데모 수시입출금",
							"1",
							"수시입출금",
							"20260313",
							"20310313",
							100_000_000L,
							50_000_000L,
							2_000_000L,
							"KRW"
						)
					)
				)
			);

		List<Account> syncedAccounts = syncAccountsUseCase.execute(7);

		ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
		verify(accountRepository).save(accountCaptor.capture());
		assertThat(syncedAccounts).hasSize(1);
		assertThat(accountCaptor.getValue().getAccountNo()).isEqualTo("0011234567890123");
		assertThat(accountCaptor.getValue().getBankCode()).isEqualTo("001");
		assertThat(accountCaptor.getValue().getBankName()).isEqualTo("한국은행");
	}
}

package com.ssafy.amagetdone.domain.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.amagetdone.domain.account.api.dto.AccountDto;
import com.ssafy.amagetdone.domain.account.infra.client.SsafyDemandDepositAccountApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositAccountDto;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfirmAccountOwnershipUseCaseTest {

	@Mock
	private FindAccountUserUseCase findAccountUserUseCase;

	@Mock
	private SsafyDemandDepositAccountApiClient ssafyDemandDepositAccountApiClient;

	@InjectMocks
	private ConfirmAccountOwnershipUseCase confirmAccountOwnershipUseCase;

	@Test
	void returnsConfirmedTrueWhenSsafyAccountBelongsToAuthenticatedUser() {
		User user = mock(User.class);
		when(user.getUserKey()).thenReturn("user-key");
		when(findAccountUserUseCase.execute(7)).thenReturn(user);
		when(ssafyDemandDepositAccountApiClient.inquireAccount(
			new DemandDepositAccountDto.AccountDetailRequest("user-key", "0011234567890123")
		)).thenReturn(new DemandDepositAccountDto.AccountDetailResponse(
			"001",
			"한국은행",
			"테스터",
			"0011234567890123",
			"주거래 입출금",
			"1",
			"수시입출금",
			"20260318",
			"20300318",
			100000000L,
			50000000L,
			250000L,
			"KRW"
		));

		AccountDto.ConfirmAccountResponse result = confirmAccountOwnershipUseCase.execute(
			7,
			new AccountDto.ConfirmAccountRequest("0011234567890123")
		);

		assertThat(result.confirmed()).isTrue();
		verify(findAccountUserUseCase).execute(7);
	}
}

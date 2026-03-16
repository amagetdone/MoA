package com.ssafy.amagetdone.domain.accountverification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.application.usecase.FindUserAccountUseCase;
import com.ssafy.amagetdone.domain.accountverification.api.dto.AccountVerificationDto;
import com.ssafy.amagetdone.domain.accountverification.infra.client.SsafyAccountVerificationApiClient;
import com.ssafy.amagetdone.domain.accountverification.infra.client.dto.AccountVerificationClientDto;
import com.ssafy.amagetdone.domain.accountverification.model.entity.AccountVerification;
import com.ssafy.amagetdone.domain.accountverification.model.repository.AccountVerificationRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RequestAccountVerificationUseCaseTest {

	@Mock
	private FindUserAccountUseCase findUserAccountUseCase;

	@Mock
	private AccountVerificationRepository accountVerificationRepository;

	@Mock
	private SsafyAccountVerificationApiClient ssafyAccountVerificationApiClient;

	@InjectMocks
	private RequestAccountVerificationUseCase requestAccountVerificationUseCase;

	@Test
	void requestVerificationStoresRequestedHistory() {
		Account account = mock(Account.class);
		User user = mock(User.class);
		AccountVerification savedVerification = AccountVerification.createRequested(
			account,
			"AMAGETDONE",
			88L,
			java.time.LocalDateTime.of(2026, 3, 13, 9, 0)
		);
		ReflectionTestUtils.setField(savedVerification, "id", 12L);
		AccountVerificationDto.RequestVerificationRequest request =
			new AccountVerificationDto.RequestVerificationRequest("0011234567890123");

		when(findUserAccountUseCase.execute(7, "0011234567890123")).thenReturn(account);
		when(account.getUser()).thenReturn(user);
		when(user.getUserKey()).thenReturn("user-key");
		when(ssafyAccountVerificationApiClient.requestVerification(any()))
			.thenReturn(new AccountVerificationClientDto.RequestVerificationResponse(88L, "0011234567890123"));
		when(accountVerificationRepository.save(any(AccountVerification.class))).thenReturn(savedVerification);

		AccountVerificationDto.RequestVerificationResponse result = requestAccountVerificationUseCase.execute(7, request);

		assertThat(result.verificationId()).isEqualTo(12L);
		assertThat(result.accountNo()).isEqualTo("0011234567890123");
		assertThat(result.transactionUniqueNo()).isEqualTo(88L);
		assertThat(result.status()).isEqualTo("REQUESTED");
		ArgumentCaptor<AccountVerification> captor = ArgumentCaptor.forClass(AccountVerification.class);
		verify(accountVerificationRepository).save(captor.capture());
		assertThat(captor.getValue().getAuthText()).isEqualTo("AMAGETDONE");
	}
}

package com.ssafy.amagetdone.domain.accountverification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.accountverification.api.dto.AccountVerificationAdminDto;
import com.ssafy.amagetdone.domain.accountverification.exception.AccountVerificationErrorCode;
import com.ssafy.amagetdone.domain.accountverification.exception.AccountVerificationException;
import com.ssafy.amagetdone.domain.accountverification.infra.client.SsafyAccountVerificationApiClient;
import com.ssafy.amagetdone.domain.accountverification.infra.client.dto.AccountVerificationClientDto;
import com.ssafy.amagetdone.domain.accountverification.model.entity.AccountVerification;
import com.ssafy.amagetdone.domain.accountverification.model.repository.AccountVerificationRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CheckAccountVerificationUseCaseTest {

	@Mock
	private AccountVerificationRepository accountVerificationRepository;

	@Mock
	private SsafyAccountVerificationApiClient ssafyAccountVerificationApiClient;

	@InjectMocks
	private CheckAccountVerificationUseCase checkAccountVerificationUseCase;

	@Test
	void checkVerificationMarksHistorySuccessAndAccountVerified() {
		Account account = mock(Account.class);
		User user = mock(User.class);
		AccountVerification verification = AccountVerification.createRequested(
			account,
			"AMAGETDONE",
			88L,
			java.time.LocalDateTime.of(2026, 3, 13, 9, 0)
		);
		ReflectionTestUtils.setField(verification, "id", 12L);
		AccountVerificationAdminDto.CheckVerificationRequest request =
			new AccountVerificationAdminDto.CheckVerificationRequest(7, "0011234567890123", "1234");

		when(accountVerificationRepository.findTopByAccount_User_IdAndAccount_AccountNoOrderByCreatedAtDesc(7, "0011234567890123"))
			.thenReturn(java.util.Optional.of(verification));
		when(account.getUser()).thenReturn(user);
		when(user.getUserKey()).thenReturn("user-key");
		when(account.getAccountNo()).thenReturn("0011234567890123");
		when(ssafyAccountVerificationApiClient.checkVerification(any()))
			.thenReturn(new AccountVerificationClientDto.CheckVerificationResponse("SUCCESS", 88L, "0011234567890123"));

		AccountVerificationAdminDto.CheckVerificationResponse result = checkAccountVerificationUseCase.execute(request);

		assertThat(result.verificationId()).isEqualTo(12L);
		assertThat(result.status()).isEqualTo("SUCCESS");
		assertThat(result.verified()).isTrue();
		verify(account).markVerified();
		verify(accountVerificationRepository).save(verification);
	}

	@Test
	void checkVerificationMarksHistoryFailedWhenCodeMismatch() {
		Account account = mock(Account.class);
		User user = mock(User.class);
		AccountVerification verification = AccountVerification.createRequested(
			account,
			"AMAGETDONE",
			88L,
			java.time.LocalDateTime.of(2026, 3, 13, 9, 0)
		);
		AccountVerificationAdminDto.CheckVerificationRequest request =
			new AccountVerificationAdminDto.CheckVerificationRequest(7, "0011234567890123", "9999");

		when(accountVerificationRepository.findTopByAccount_User_IdAndAccount_AccountNoOrderByCreatedAtDesc(7, "0011234567890123"))
			.thenReturn(java.util.Optional.of(verification));
		when(account.getUser()).thenReturn(user);
		when(user.getUserKey()).thenReturn("user-key");
		when(account.getAccountNo()).thenReturn("0011234567890123");
		when(ssafyAccountVerificationApiClient.checkVerification(any()))
			.thenThrow(new AccountVerificationException(AccountVerificationErrorCode.AUTH_CODE_MISMATCH));

		assertThatThrownBy(() -> checkAccountVerificationUseCase.execute(request))
			.isInstanceOf(AccountVerificationException.class)
			.extracting("errorCode")
			.isEqualTo(AccountVerificationErrorCode.AUTH_CODE_MISMATCH);

		verify(accountVerificationRepository).save(verification);
		assertThat(verification.getStatus().name()).isEqualTo("FAILED");
		assertThat(verification.getFailureReason()).isEqualTo(AccountVerificationErrorCode.AUTH_CODE_MISMATCH.message());
	}
}

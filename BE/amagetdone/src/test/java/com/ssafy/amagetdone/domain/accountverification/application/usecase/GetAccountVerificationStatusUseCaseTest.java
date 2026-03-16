package com.ssafy.amagetdone.domain.accountverification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.accountverification.api.dto.AccountVerificationAdminDto;
import com.ssafy.amagetdone.domain.accountverification.model.entity.AccountVerification;
import com.ssafy.amagetdone.domain.accountverification.model.entity.AccountVerificationStatus;
import com.ssafy.amagetdone.domain.accountverification.model.repository.AccountVerificationRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GetAccountVerificationStatusUseCaseTest {

	@Mock
	private AccountVerificationRepository accountVerificationRepository;

	@InjectMocks
	private GetAccountVerificationStatusUseCase getAccountVerificationStatusUseCase;

	@Test
	void statusLookupReturnsLatestVerificationRecord() {
		Account account = mock(Account.class);
		AccountVerification verification = AccountVerification.createRequested(
			account,
			"AMAGETDONE",
			88L,
			java.time.LocalDateTime.of(2026, 3, 13, 9, 0)
		);
		ReflectionTestUtils.setField(verification, "id", 12L);
		ReflectionTestUtils.setField(verification, "status", AccountVerificationStatus.SUCCESS);
		ReflectionTestUtils.setField(
			verification,
			"verifiedAt",
			java.time.LocalDateTime.of(2026, 3, 13, 9, 3)
		);

		when(accountVerificationRepository.findTopByAccount_User_IdAndAccount_AccountNoOrderByCreatedAtDesc(7, "0011234567890123"))
			.thenReturn(java.util.Optional.of(verification));
		when(account.isVerified()).thenReturn(true);
		when(account.getAccountNo()).thenReturn("0011234567890123");
		when(account.getUser()).thenReturn(mock(com.ssafy.amagetdone.domain.member.model.entity.User.class));
		when(account.getUser().getId()).thenReturn(7);

		AccountVerificationAdminDto.VerificationStatusResponse result =
			getAccountVerificationStatusUseCase.execute(7, "0011234567890123");

		assertThat(result.verificationId()).isEqualTo(12L);
		assertThat(result.userId()).isEqualTo(7);
		assertThat(result.status()).isEqualTo("SUCCESS");
		assertThat(result.verified()).isTrue();
		assertThat(result.authText()).isEqualTo("AMAGETDONE");
	}
}

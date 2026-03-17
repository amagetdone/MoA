package com.ssafy.amagetdone.domain.accountverification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.accountverification.api.dto.AccountVerificationDto;
import com.ssafy.amagetdone.domain.accountverification.model.entity.AccountVerification;
import com.ssafy.amagetdone.domain.accountverification.model.entity.AccountVerificationStatus;
import com.ssafy.amagetdone.domain.accountverification.model.repository.AccountVerificationRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;

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

	@Mock
	private AccountRepository accountRepository;

	@InjectMocks
	private GetAccountVerificationStatusUseCase getAccountVerificationStatusUseCase;

	@Test
	void statusLookupReturnsLatestVerificationRecord() {
		User user = mock(User.class);

		AccountVerification verification = AccountVerification.createRequested(
			user,
			"0011234567890123",
			"AMAGETDONE",
			88L,
			LocalDateTime.of(2026, 3, 13, 9, 0)
		);
		ReflectionTestUtils.setField(verification, "id", 12L);
		ReflectionTestUtils.setField(verification, "status", AccountVerificationStatus.SUCCESS);
		ReflectionTestUtils.setField(verification, "verifiedAt", LocalDateTime.of(2026, 3, 13, 9, 3));

		when(accountVerificationRepository.findTopByUser_IdAndAccountNoOrderByCreatedAtDesc(7, "0011234567890123"))
			.thenReturn(Optional.of(verification));

		Account account = mock(Account.class);
		when(account.isVerified()).thenReturn(true);
		when(accountRepository.findByUserIdAndAccountNo(7, "0011234567890123"))
			.thenReturn(Optional.of(account));

		AccountVerificationDto.VerificationStatusResponse result =
			getAccountVerificationStatusUseCase.execute(7, "0011234567890123");

		assertThat(result.verificationId()).isEqualTo(12L);
		assertThat(result.status()).isEqualTo("SUCCESS");
		assertThat(result.verified()).isTrue();
		assertThat(result.authText()).isEqualTo("AMAGETDONE");
	}
}

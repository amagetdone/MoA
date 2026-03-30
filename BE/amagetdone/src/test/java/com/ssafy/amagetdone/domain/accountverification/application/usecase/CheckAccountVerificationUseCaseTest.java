// Responsibility: Verifies account verification success and failure paths, including relinking deleted accounts.
package com.ssafy.amagetdone.domain.accountverification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import com.ssafy.amagetdone.domain.account.infra.client.SsafyDemandDepositAccountApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositAccountDto;
import com.ssafy.amagetdone.domain.account.application.usecase.SyncLinkedCardsUseCase;
import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.accountverification.api.dto.AccountVerificationDto;
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
	private AccountRepository accountRepository;

	@Mock
	private SsafyAccountVerificationApiClient ssafyAccountVerificationApiClient;

	@Mock
	private SsafyDemandDepositAccountApiClient ssafyDemandDepositAccountApiClient;

	@Mock
	private SyncLinkedCardsUseCase syncLinkedCardsUseCase;

	@InjectMocks
	private CheckAccountVerificationUseCase checkAccountVerificationUseCase;

	@Test
	void checkVerificationMarksHistorySuccessAndRegistersAccount() {
		User user = mock(User.class);
		when(user.getId()).thenReturn(7);
		when(user.getUserKey()).thenReturn("user-key");

		AccountVerification verification = AccountVerification.createRequested(
			user,
			"0011234567890123",
			"AMAGETDONE",
			88L,
			LocalDateTime.of(2026, 3, 13, 9, 0)
		);
		ReflectionTestUtils.setField(verification, "id", 12L);

		AccountVerificationDto.CheckVerificationRequest request =
			new AccountVerificationDto.CheckVerificationRequest("0011234567890123", "1234");

		when(accountVerificationRepository.findTopByUser_IdAndAccountNoOrderByCreatedAtDesc(7, "0011234567890123"))
			.thenReturn(Optional.of(verification));
		when(ssafyAccountVerificationApiClient.checkVerification(any()))
			.thenReturn(new AccountVerificationClientDto.CheckVerificationResponse("SUCCESS", 88L, "0011234567890123"));

		// Account does not exist yet; will be created from Ssafy API data
		when(accountRepository.findByUserIdAndAccountNo(7, "0011234567890123"))
			.thenReturn(Optional.empty());

		DemandDepositAccountDto.AccountDetailResponse accountDetail = new DemandDepositAccountDto.AccountDetailResponse(
			"001", "싸피은행", "테스트유저", "0011234567890123", "수시입출금", "001", "수시입출금",
			"20260101", "20290101", 10000000L, 5000000L, 100000L, "KRW"
		);
		when(ssafyDemandDepositAccountApiClient.inquireAccount(any())).thenReturn(accountDetail);

		when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

		AccountVerificationDto.CheckVerificationResponse result =
			checkAccountVerificationUseCase.execute(7, request);

		assertThat(result.verificationId()).isEqualTo(12L);
		assertThat(result.status()).isEqualTo("SUCCESS");
		assertThat(result.verified()).isTrue();
		verify(syncLinkedCardsUseCase).execute(7, "user-key", "0011234567890123");
		verify(accountVerificationRepository).save(verification);
		verify(accountRepository).save(any(Account.class));
	}

	@Test
	void checkVerificationReactivatesExistingLinkedAccount() {
		User user = mock(User.class);
		when(user.getId()).thenReturn(7);
		when(user.getUserKey()).thenReturn("user-key");

		AccountVerification verification = AccountVerification.createRequested(
			user,
			"0011234567890123",
			"AMAGETDONE",
			88L,
			LocalDateTime.of(2026, 3, 13, 9, 0)
		);
		ReflectionTestUtils.setField(verification, "id", 12L);

		Account existingAccount = Account.create(
			user,
			"0011234567890123",
			null,
			"데모 계좌",
			"수시입출금",
			"001",
			"한국은행",
			"KRW"
		);
		existingAccount.markInactive();

		when(accountVerificationRepository.findTopByUser_IdAndAccountNoOrderByCreatedAtDesc(7, "0011234567890123"))
			.thenReturn(Optional.of(verification));
		when(ssafyAccountVerificationApiClient.checkVerification(any()))
			.thenReturn(new AccountVerificationClientDto.CheckVerificationResponse("SUCCESS", 88L, "0011234567890123"));
		when(accountRepository.findByUserIdAndAccountNo(7, "0011234567890123"))
			.thenReturn(Optional.of(existingAccount));
		when(ssafyDemandDepositAccountApiClient.inquireAccount(any())).thenReturn(new DemandDepositAccountDto.AccountDetailResponse(
			"001",
			"한국은행",
			"테스터",
			"0011234567890123",
			"데모 계좌",
			"1",
			"수시입출금",
			"20260318",
			"20300318",
			100000000L,
			50000000L,
			250000L,
			"KRW"
		));
		when(accountRepository.save(existingAccount)).thenReturn(existingAccount);

		AccountVerificationDto.CheckVerificationResponse result = checkAccountVerificationUseCase.execute(
			7,
			new AccountVerificationDto.CheckVerificationRequest("0011234567890123", "1234")
		);

		assertThat(result.verified()).isTrue();
		assertThat(existingAccount.isActive()).isTrue();
		assertThat(existingAccount.isDeleted()).isFalse();
		verify(syncLinkedCardsUseCase).execute(7, "user-key", "0011234567890123");
	}

	@Test
	void checkVerificationRestoresSoftDeletedAccount() {
		User user = mock(User.class);
		when(user.getId()).thenReturn(7);
		when(user.getUserKey()).thenReturn("user-key");

		AccountVerification verification = AccountVerification.createRequested(
			user,
			"0011234567890123",
			"AMAGETDONE",
			88L,
			LocalDateTime.of(2026, 3, 13, 9, 0)
		);
		ReflectionTestUtils.setField(verification, "id", 12L);

		Account existingAccount = Account.create(
			user,
			"0011234567890123",
			null,
			"삭제된 계좌",
			"수시입출금",
			"001",
			"한국은행",
			"KRW"
		);
		existingAccount.softDelete();

		when(accountVerificationRepository.findTopByUser_IdAndAccountNoOrderByCreatedAtDesc(7, "0011234567890123"))
			.thenReturn(Optional.of(verification));
		when(ssafyAccountVerificationApiClient.checkVerification(any()))
			.thenReturn(new AccountVerificationClientDto.CheckVerificationResponse("SUCCESS", 88L, "0011234567890123"));
		when(accountRepository.findByUserIdAndAccountNo(7, "0011234567890123"))
			.thenReturn(Optional.of(existingAccount));
		when(ssafyDemandDepositAccountApiClient.inquireAccount(any())).thenReturn(new DemandDepositAccountDto.AccountDetailResponse(
			"001",
			"한국은행",
			"테스터",
			"0011234567890123",
			"삭제된 계좌",
			"1",
			"수시입출금",
			"20260318",
			"20300318",
			100000000L,
			50000000L,
			250000L,
			"KRW"
		));
		when(accountRepository.save(existingAccount)).thenReturn(existingAccount);

		AccountVerificationDto.CheckVerificationResponse result = checkAccountVerificationUseCase.execute(
			7,
			new AccountVerificationDto.CheckVerificationRequest("0011234567890123", "1234")
		);

		assertThat(result.verified()).isTrue();
		assertThat(existingAccount.isVerified()).isTrue();
		assertThat(existingAccount.isActive()).isTrue();
		assertThat(existingAccount.isDeleted()).isFalse();
		verify(syncLinkedCardsUseCase).execute(7, "user-key", "0011234567890123");
	}

	@Test
	void checkVerificationMarksHistoryFailedWhenCodeMismatch() {
		User user = mock(User.class);
		when(user.getUserKey()).thenReturn("user-key");

		AccountVerification verification = AccountVerification.createRequested(
			user,
			"0011234567890123",
			"AMAGETDONE",
			88L,
			LocalDateTime.of(2026, 3, 13, 9, 0)
		);

		AccountVerificationDto.CheckVerificationRequest request =
			new AccountVerificationDto.CheckVerificationRequest("0011234567890123", "9999");

		when(accountVerificationRepository.findTopByUser_IdAndAccountNoOrderByCreatedAtDesc(7, "0011234567890123"))
			.thenReturn(Optional.of(verification));
		when(ssafyAccountVerificationApiClient.checkVerification(any()))
			.thenThrow(new AccountVerificationException(AccountVerificationErrorCode.AUTH_CODE_MISMATCH));

		assertThatThrownBy(() -> checkAccountVerificationUseCase.execute(7, request))
			.isInstanceOf(AccountVerificationException.class)
			.extracting("errorCode")
			.isEqualTo(AccountVerificationErrorCode.AUTH_CODE_MISMATCH);

		verify(accountVerificationRepository).save(verification);
		assertThat(verification.getStatus().name()).isEqualTo("FAILED");
		assertThat(verification.getFailureReason()).isEqualTo(AccountVerificationErrorCode.AUTH_CODE_MISMATCH.message());
		verify(syncLinkedCardsUseCase, never()).execute(any(), any(), any());
	}

	@Test
	void checkVerificationSucceedsWhenSsafyCardListIsEmpty() {
		User user = mock(User.class);
		when(user.getId()).thenReturn(7);
		when(user.getUserKey()).thenReturn("user-key");

		AccountVerification verification = AccountVerification.createRequested(
			user,
			"0011234567890123",
			"AMAGETDONE",
			88L,
			LocalDateTime.of(2026, 3, 13, 9, 0)
		);
		ReflectionTestUtils.setField(verification, "id", 12L);

		when(accountVerificationRepository.findTopByUser_IdAndAccountNoOrderByCreatedAtDesc(7, "0011234567890123"))
			.thenReturn(Optional.of(verification));
		when(ssafyAccountVerificationApiClient.checkVerification(any()))
			.thenReturn(new AccountVerificationClientDto.CheckVerificationResponse("SUCCESS", 88L, "0011234567890123"));
		when(accountRepository.findByUserIdAndAccountNo(7, "0011234567890123"))
			.thenReturn(Optional.empty());
		when(ssafyDemandDepositAccountApiClient.inquireAccount(any())).thenReturn(new DemandDepositAccountDto.AccountDetailResponse(
			"001",
			"싸피은행",
			"테스트유저",
			"0011234567890123",
			"수시입출금",
			"001",
			"수시입출금",
			"20260101",
			"20290101",
			10000000L,
			5000000L,
			100000L,
			"KRW"
		));

		when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

		AccountVerificationDto.CheckVerificationResponse result = checkAccountVerificationUseCase.execute(
			7,
			new AccountVerificationDto.CheckVerificationRequest("0011234567890123", "1234")
		);

		assertThat(result.status()).isEqualTo("SUCCESS");
		verify(syncLinkedCardsUseCase).execute(7, "user-key", "0011234567890123");
	}

	@Test
	void checkVerificationFailsWhenLinkedCardSyncFails() {
		User user = mock(User.class);
		when(user.getId()).thenReturn(7);
		when(user.getUserKey()).thenReturn("user-key");

		AccountVerification verification = AccountVerification.createRequested(
			user,
			"0011234567890123",
			"AMAGETDONE",
			88L,
			LocalDateTime.of(2026, 3, 13, 9, 0)
		);

		when(accountVerificationRepository.findTopByUser_IdAndAccountNoOrderByCreatedAtDesc(7, "0011234567890123"))
			.thenReturn(Optional.of(verification));
		when(ssafyAccountVerificationApiClient.checkVerification(any()))
			.thenReturn(new AccountVerificationClientDto.CheckVerificationResponse("SUCCESS", 88L, "0011234567890123"));
		when(accountRepository.findByUserIdAndAccountNo(7, "0011234567890123"))
			.thenReturn(Optional.empty());
		when(ssafyDemandDepositAccountApiClient.inquireAccount(any())).thenReturn(new DemandDepositAccountDto.AccountDetailResponse(
			"001",
			"싸피은행",
			"테스트유저",
			"0011234567890123",
			"수시입출금",
			"001",
			"수시입출금",
			"20260101",
			"20290101",
			10000000L,
			5000000L,
			100000L,
			"KRW"
		));
		when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
		org.mockito.Mockito.doThrow(new AccountException(AccountErrorCode.LINKED_CARD_SYNC_FAILED))
			.when(syncLinkedCardsUseCase)
			.execute(7, "user-key", "0011234567890123");

		assertThatThrownBy(() -> checkAccountVerificationUseCase.execute(
			7,
			new AccountVerificationDto.CheckVerificationRequest("0011234567890123", "1234")
		))
			.isInstanceOf(AccountException.class)
			.extracting("errorCode")
			.isEqualTo(AccountErrorCode.LINKED_CARD_SYNC_FAILED);
	}
}

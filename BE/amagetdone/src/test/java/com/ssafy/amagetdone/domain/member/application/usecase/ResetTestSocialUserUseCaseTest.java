package com.ssafy.amagetdone.domain.member.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;

import com.ssafy.amagetdone.domain.member.infra.client.MockSocialAuthClient;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.account.model.repository.CardRepository;
import com.ssafy.amagetdone.domain.accountverification.model.repository.AccountVerificationRepository;
import com.ssafy.amagetdone.domain.member.model.entity.SocialAccount;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.RefreshTokenStore;
import com.ssafy.amagetdone.domain.member.model.repository.SocialAccountRepository;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResetTestSocialUserUseCaseTest {

	@Mock
	private SocialAccountRepository socialAccountRepository;

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private CardRepository cardRepository;

	@Mock
	private ResetUserFlowStateUseCase resetUserFlowStateUseCase;

	@Mock
	private AccountVerificationRepository accountVerificationRepository;

	@Mock
	private RefreshTokenStore refreshTokenStore;

	private ResetTestSocialUserUseCase createUseCase() {
		return new ResetTestSocialUserUseCase(
			socialAccountRepository,
			resetUserFlowStateUseCase,
			accountRepository,
			cardRepository,
			accountVerificationRepository,
			refreshTokenStore
		);
	}

	@Test
	void executeResetsTestUserStateAndInvalidatesTokens() {
		ResetTestSocialUserUseCase useCase = createUseCase();
		User user = User.createPendingSocialUser();
		setUserId(user, 1);
		user.markProfileEntered("moa", "moa@example.com", LocalDate.of(1997, 3, 1), true);
		user.updateUserKey("user-key");
		user.activate();
		SocialAccount socialAccount = SocialAccount.create(user, SocialProvider.TEST, MockSocialAuthClient.providerUidFor(1));

		given(socialAccountRepository.findByUser_IdAndProvider(1, SocialProvider.TEST))
			.willReturn(Optional.of(socialAccount));

		useCase.execute(1);

		verify(resetUserFlowStateUseCase).execute(1);
		verify(accountVerificationRepository).deleteAllByUserId(1);
		verify(cardRepository).deleteAllByUserId(1);
		verify(accountRepository).deleteByUserId(1);
		verify(refreshTokenStore).invalidateAll(1);
		assertEquals("TEMP", user.getStatus().name());
		assertNull(user.getEmail());
		assertNull(user.getUserKey());
	}

	@Test
	void executeIsIdempotentWhenTestUserDoesNotExist() {
		ResetTestSocialUserUseCase useCase = createUseCase();
		given(socialAccountRepository.findByUser_IdAndProvider(1, SocialProvider.TEST))
			.willReturn(Optional.empty());

		useCase.execute(1);

		verifyNoInteractions(resetUserFlowStateUseCase, accountRepository, accountVerificationRepository, refreshTokenStore);
	}

	private void setUserId(User user, int id) {
		try {
			Field idField = User.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(user, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}

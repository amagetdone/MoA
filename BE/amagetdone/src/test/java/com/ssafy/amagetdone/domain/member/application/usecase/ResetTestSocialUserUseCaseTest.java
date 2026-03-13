package com.ssafy.amagetdone.domain.member.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;

import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.member.model.entity.SocialAccount;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.RefreshTokenStore;
import com.ssafy.amagetdone.domain.member.model.repository.SocialAccountRepository;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;

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
	private PlanRepository planRepository;

	@Mock
	private PlanWeekAllocationRepository planWeekAllocationRepository;

	@Mock
	private RefreshTokenStore refreshTokenStore;

	@Test
	void executeResetsTestUserStateAndInvalidatesTokens() {
		ResetTestSocialUserUseCase useCase = new ResetTestSocialUserUseCase(
			socialAccountRepository,
			accountRepository,
			planRepository,
			planWeekAllocationRepository,
			refreshTokenStore
		);
		User user = User.createPendingSocialUser();
		setUserId(user, 1);
		user.markProfileEntered("moa", "moa@example.com", LocalDate.of(1997, 3, 1), true);
		user.updateUserKey("user-key");
		user.activate();
		SocialAccount socialAccount = SocialAccount.create(user, SocialProvider.TEST, "test-social-user-001");

		given(socialAccountRepository.findByProviderAndProviderUid(SocialProvider.TEST, "test-social-user-001"))
			.willReturn(Optional.of(socialAccount));

		useCase.execute();

		verify(planWeekAllocationRepository).deleteByPlanUserId(1);
		verify(planRepository).deleteByUserId(1);
		verify(accountRepository).deleteByUserId(1);
		verify(refreshTokenStore).invalidateAll(1);
		assertEquals("TEMP", user.getStatus().name());
		assertNull(user.getEmail());
		assertNull(user.getUserKey());
	}

	@Test
	void executeIsIdempotentWhenTestUserDoesNotExist() {
		ResetTestSocialUserUseCase useCase = new ResetTestSocialUserUseCase(
			socialAccountRepository,
			accountRepository,
			planRepository,
			planWeekAllocationRepository,
			refreshTokenStore
		);
		given(socialAccountRepository.findByProviderAndProviderUid(SocialProvider.TEST, "test-social-user-001"))
			.willReturn(Optional.empty());

		useCase.execute();

		verifyNoInteractions(accountRepository, planRepository, planWeekAllocationRepository, refreshTokenStore);
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

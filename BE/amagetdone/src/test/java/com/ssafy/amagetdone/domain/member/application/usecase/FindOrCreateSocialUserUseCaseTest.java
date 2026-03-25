package com.ssafy.amagetdone.domain.member.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;

import com.ssafy.amagetdone.domain.member.infra.client.SocialUserIdentity;
import com.ssafy.amagetdone.domain.member.model.entity.SocialAccount;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.SocialAccountRepository;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;
import com.ssafy.amagetdone.domain.member.support.config.TestSocialUserProperties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindOrCreateSocialUserUseCaseTest {

	@Mock
	private SocialAccountRepository socialAccountRepository;

	@Mock
	private UserRepository userRepository;

	@Test
	void createDoesNotGenerateSyntheticFinancialUserKey() {
		FindOrCreateSocialUserUseCase useCase = new FindOrCreateSocialUserUseCase(
			socialAccountRepository,
			userRepository,
			testSocialUserProperties()
		);
		SocialUserIdentity identity = new SocialUserIdentity(SocialProvider.KAKAO, "provider-uid");
		User savedUser = User.createPendingSocialUser();
		SocialAccount savedSocialAccount = SocialAccount.create(savedUser, SocialProvider.KAKAO, "provider-uid");

		given(socialAccountRepository.findByProviderAndProviderUid(SocialProvider.KAKAO, "provider-uid"))
			.willReturn(Optional.empty());
		given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(socialAccountRepository.save(any(SocialAccount.class))).willReturn(savedSocialAccount);

		FindOrCreateSocialUserUseCase.Result result = useCase.execute(identity);

		assertFalse(result.isNewUser() && result.user().getUserKey() != null);
		assertNull(result.user().getUserKey());
		assertEquals("TEMP", result.user().getStatus().name());
		verify(socialAccountRepository).findByProviderAndProviderUid(eq(SocialProvider.KAKAO), eq("provider-uid"));
	}

	@Test
	void createInitializesConfiguredFieldsForRequestedTestSocialUser() {
		FindOrCreateSocialUserUseCase useCase = new FindOrCreateSocialUserUseCase(
			socialAccountRepository,
			userRepository,
			testSocialUserProperties()
		);
		SocialUserIdentity identity = new SocialUserIdentity(SocialProvider.TEST, "test-social-user-002");
		User savedUser = User.createPendingSocialUser();
		SocialAccount savedSocialAccount = SocialAccount.create(savedUser, SocialProvider.TEST, "test-social-user-002");

		given(socialAccountRepository.findByProviderAndProviderUid(SocialProvider.TEST, "test-social-user-002"))
			.willReturn(Optional.empty());
		given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(socialAccountRepository.save(any(SocialAccount.class))).willReturn(savedSocialAccount);

		FindOrCreateSocialUserUseCase.Result result = useCase.execute(identity);

		assertEquals("tester2", result.user().getNickname());
		assertEquals("test2@test.com", result.user().getEmail());
		assertEquals(LocalDate.of(1997, 3, 1), result.user().getBirthDate());
		assertEquals(Boolean.TRUE, result.user().getHasFixedIncome());
		assertNull(result.user().getUserKey());
		assertEquals("ACTIVE", result.user().getStatus().name());
		assertTrue(result.user().isProfileCompleted());
		assertFalse(result.isNewUser() && result.user().getUserKey() != null);
	}

	@Test
	void executeBackfillsConfiguredFieldsForExistingTestSocialUser() {
		FindOrCreateSocialUserUseCase useCase = new FindOrCreateSocialUserUseCase(
			socialAccountRepository,
			userRepository,
			testSocialUserProperties()
		);
		User existingUser = User.createPendingSocialUser();
		SocialAccount existingSocialAccount = SocialAccount.create(existingUser, SocialProvider.TEST, "test-social-user-005");
		SocialUserIdentity identity = new SocialUserIdentity(SocialProvider.TEST, "test-social-user-005");

		given(socialAccountRepository.findByProviderAndProviderUid(SocialProvider.TEST, "test-social-user-005"))
			.willReturn(Optional.of(existingSocialAccount));

		FindOrCreateSocialUserUseCase.Result result = useCase.execute(identity);

		assertFalse(result.isNewUser());
		assertEquals("tester5", result.user().getNickname());
		assertEquals("test5@test.com", result.user().getEmail());
		assertNotNull(result.user().getBirthDate());
		assertEquals(Boolean.TRUE, result.user().getHasFixedIncome());
		assertEquals("ACTIVE", result.user().getStatus().name());
		assertTrue(result.user().isProfileCompleted());
		assertNull(result.user().getUserKey());
	}

	private TestSocialUserProperties testSocialUserProperties() {
		TestSocialUserProperties properties = new TestSocialUserProperties();
		properties.setNickname("tester");
		properties.setEmail("test@test.com");
		return properties;
	}
}

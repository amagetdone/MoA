package com.ssafy.amagetdone.domain.member.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
	void createInitializesConfiguredFieldsForTestSocialUser() {
		FindOrCreateSocialUserUseCase useCase = new FindOrCreateSocialUserUseCase(
			socialAccountRepository,
			userRepository,
			testSocialUserProperties()
		);
		SocialUserIdentity identity = new SocialUserIdentity(SocialProvider.TEST, "test-social-user-001");
		User savedUser = User.createPendingSocialUser();
		SocialAccount savedSocialAccount = SocialAccount.create(savedUser, SocialProvider.TEST, "test-social-user-001");

		given(socialAccountRepository.findByProviderAndProviderUid(SocialProvider.TEST, "test-social-user-001"))
			.willReturn(Optional.empty());
		given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(socialAccountRepository.save(any(SocialAccount.class))).willReturn(savedSocialAccount);

		FindOrCreateSocialUserUseCase.Result result = useCase.execute(identity);

		assertEquals("tester", result.user().getNickname());
		assertEquals("ssafy123@tester.com", result.user().getEmail());
		assertEquals("04fa9ec5-3e42-4847-84ae-34c855cdd1b6", result.user().getUserKey());
		assertEquals("TEMP", result.user().getStatus().name());
		assertFalse(result.isNewUser() && result.user().getUserKey() == null);
	}

	@Test
	void executeBackfillsConfiguredFieldsForExistingTestSocialUser() {
		FindOrCreateSocialUserUseCase useCase = new FindOrCreateSocialUserUseCase(
			socialAccountRepository,
			userRepository,
			testSocialUserProperties()
		);
		User existingUser = User.createPendingSocialUser();
		SocialAccount existingSocialAccount = SocialAccount.create(existingUser, SocialProvider.TEST, "test-social-user-001");
		SocialUserIdentity identity = new SocialUserIdentity(SocialProvider.TEST, "test-social-user-001");

		given(socialAccountRepository.findByProviderAndProviderUid(SocialProvider.TEST, "test-social-user-001"))
			.willReturn(Optional.of(existingSocialAccount));

		FindOrCreateSocialUserUseCase.Result result = useCase.execute(identity);

		assertFalse(result.isNewUser());
		assertEquals("tester", result.user().getNickname());
		assertEquals("ssafy123@tester.com", result.user().getEmail());
		assertEquals("04fa9ec5-3e42-4847-84ae-34c855cdd1b6", result.user().getUserKey());
	}

	private TestSocialUserProperties testSocialUserProperties() {
		TestSocialUserProperties properties = new TestSocialUserProperties();
		properties.setNickname("tester");
		properties.setEmail("ssafy123@tester.com");
		properties.setUserApiKey("04fa9ec5-3e42-4847-84ae-34c855cdd1b6");
		return properties;
	}
}

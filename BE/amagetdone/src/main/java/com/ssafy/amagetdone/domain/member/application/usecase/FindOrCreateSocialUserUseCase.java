package com.ssafy.amagetdone.domain.member.application.usecase;

import com.ssafy.amagetdone.domain.member.infra.client.MockSocialAuthClient;
import com.ssafy.amagetdone.domain.member.infra.client.SocialUserIdentity;
import com.ssafy.amagetdone.domain.member.model.entity.SocialAccount;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.SocialAccountRepository;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;
import com.ssafy.amagetdone.domain.member.support.config.TestSocialUserProperties;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FindOrCreateSocialUserUseCase {

	private final SocialAccountRepository socialAccountRepository;
	private final UserRepository userRepository;
	private final TestSocialUserProperties testSocialUserProperties;

	public FindOrCreateSocialUserUseCase(
		SocialAccountRepository socialAccountRepository,
		UserRepository userRepository,
		TestSocialUserProperties testSocialUserProperties
	) {
		this.socialAccountRepository = socialAccountRepository;
		this.userRepository = userRepository;
		this.testSocialUserProperties = testSocialUserProperties;
	}

	@Transactional
	public Result execute(SocialUserIdentity identity) {
		return socialAccountRepository.findByProviderAndProviderUid(identity.provider(), identity.providerUid())
			.map(existing -> updateIfTestUser(identity, existing))
			.orElseGet(() -> create(identity));
	}

	private Result create(SocialUserIdentity identity) {
		User user = User.createPendingSocialUser();
		applyConfiguredTestSocialProfile(identity, user);
		User savedUser = userRepository.save(user);
		SocialAccount socialAccount = socialAccountRepository.save(
			SocialAccount.create(savedUser, identity.provider(), identity.providerUid())
		);
		return new Result(savedUser, socialAccount, true);
	}

	private Result updateIfTestUser(SocialUserIdentity identity, SocialAccount existing) {
		User user = existing.getUser();
		applyConfiguredTestSocialProfile(identity, user);
		return new Result(user, existing, false);
	}

	private void applyConfiguredTestSocialProfile(SocialUserIdentity identity, User user) {
		if (identity.provider() != SocialProvider.TEST) {
			return;
		}
		int testUserId = MockSocialAuthClient.resolveTestUserId(identity.providerUid());
		user.markProfileEntered(
			testSocialUserProperties.resolveNickname(testUserId),
			testSocialUserProperties.resolveEmail(testUserId),
			testSocialUserProperties.resolveBirthDate(testUserId),
			testSocialUserProperties.resolveHasFixedIncome(testUserId)
		);
		user.activate();
	}

	public record Result(
		User user,
		SocialAccount socialAccount,
		boolean isNewUser
	) {
	}
}

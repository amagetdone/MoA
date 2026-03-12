package com.ssafy.amagetdone.domain.member.application.usecase;

import com.ssafy.amagetdone.domain.member.infra.client.SocialUserIdentity;
import com.ssafy.amagetdone.domain.member.model.entity.SocialAccount;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.SocialAccountRepository;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FindOrCreateSocialUserUseCase {

	private final SocialAccountRepository socialAccountRepository;
	private final UserRepository userRepository;

	public FindOrCreateSocialUserUseCase(
		SocialAccountRepository socialAccountRepository,
		UserRepository userRepository
	) {
		this.socialAccountRepository = socialAccountRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public Result execute(SocialUserIdentity identity) {
		return socialAccountRepository.findByProviderAndProviderUid(identity.provider(), identity.providerUid())
			.map(existing -> new Result(existing.getUser(), existing, false))
			.orElseGet(() -> create(identity));
	}

	private Result create(SocialUserIdentity identity) {
		User user = userRepository.save(User.createPendingSocialUser());
		SocialAccount socialAccount = socialAccountRepository.save(
			SocialAccount.create(user, identity.provider(), identity.providerUid())
		);
		return new Result(user, socialAccount, true);
	}

	public record Result(
		User user,
		SocialAccount socialAccount,
		boolean isNewUser
	) {
	}
}

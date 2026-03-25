package com.ssafy.amagetdone.domain.member.application.usecase;

import java.util.stream.IntStream;

import com.ssafy.amagetdone.domain.member.infra.client.MockSocialAuthClient;
import com.ssafy.amagetdone.domain.member.infra.client.SocialUserIdentity;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EnsureTestSocialUsersInitializedUseCase {

	private static final int DEFAULT_TEST_USER_COUNT = 5;

	private final FindOrCreateSocialUserUseCase findOrCreateSocialUserUseCase;

	public EnsureTestSocialUsersInitializedUseCase(FindOrCreateSocialUserUseCase findOrCreateSocialUserUseCase) {
		this.findOrCreateSocialUserUseCase = findOrCreateSocialUserUseCase;
	}

	@Transactional
	public void execute() {
		IntStream.rangeClosed(1, DEFAULT_TEST_USER_COUNT)
			.mapToObj(this::toIdentity)
			.forEach(findOrCreateSocialUserUseCase::execute);
	}

	private SocialUserIdentity toIdentity(int testUserId) {
		return new SocialUserIdentity(
			SocialProvider.TEST,
			MockSocialAuthClient.providerUidFor(testUserId)
		);
	}
}

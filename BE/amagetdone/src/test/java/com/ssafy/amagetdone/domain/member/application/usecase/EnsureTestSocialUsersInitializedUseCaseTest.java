package com.ssafy.amagetdone.domain.member.application.usecase;

import static org.mockito.BDDMockito.then;

import java.util.stream.IntStream;

import com.ssafy.amagetdone.domain.member.infra.client.MockSocialAuthClient;
import com.ssafy.amagetdone.domain.member.infra.client.SocialUserIdentity;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnsureTestSocialUsersInitializedUseCaseTest {

	@Mock
	private FindOrCreateSocialUserUseCase findOrCreateSocialUserUseCase;

	@Test
	void executeEnsuresFiveDefaultTestUsersExist() {
		EnsureTestSocialUsersInitializedUseCase useCase =
			new EnsureTestSocialUsersInitializedUseCase(findOrCreateSocialUserUseCase);

		useCase.execute();

		IntStream.rangeClosed(1, 5).forEach(testUserId ->
			then(findOrCreateSocialUserUseCase).should().execute(new SocialUserIdentity(
				SocialProvider.TEST,
				MockSocialAuthClient.providerUidFor(testUserId)
			))
		);
	}
}

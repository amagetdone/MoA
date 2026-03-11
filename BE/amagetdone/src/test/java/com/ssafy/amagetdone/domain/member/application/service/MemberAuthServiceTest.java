package com.ssafy.amagetdone.domain.member.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import com.ssafy.amagetdone.domain.member.api.dto.SocialAuthDto;
import com.ssafy.amagetdone.domain.member.application.usecase.FindOrCreateSocialUserUseCase;
import com.ssafy.amagetdone.domain.member.application.usecase.IssueMemberTokensUseCase;
import com.ssafy.amagetdone.domain.member.infra.client.SocialAuthClient;
import com.ssafy.amagetdone.domain.member.infra.client.SocialUserIdentity;
import com.ssafy.amagetdone.domain.member.model.entity.SocialAccount;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;
import com.ssafy.amagetdone.domain.member.support.util.MemberTokenPair;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberAuthServiceTest {

	@Mock
	private SocialAuthClient kakaoSocialAuthClient;

	@Mock
	private SocialAuthClient googleSocialAuthClient;

	@Mock
	private FindOrCreateSocialUserUseCase findOrCreateSocialUserUseCase;

	@Mock
	private IssueMemberTokensUseCase issueMemberTokensUseCase;

	@Test
	void authenticateCreatesNewMemberWhenProviderAccountDoesNotExist() {
		MemberAuthService memberAuthService = new MemberAuthService(
			List.of(kakaoSocialAuthClient, googleSocialAuthClient),
			findOrCreateSocialUserUseCase,
			issueMemberTokensUseCase
		);
		SocialAuthDto.Request request = new SocialAuthDto.Request("provider-access-token", null);
		SocialUserIdentity identity = new SocialUserIdentity(SocialProvider.KAKAO, "kakao-user-1");
		User user = User.createPendingSocialUser("USER-001");
		setUserId(user, 1);
		SocialAccount socialAccount = SocialAccount.create(user, SocialProvider.KAKAO, "kakao-user-1");
		MemberTokenPair tokenPair = new MemberTokenPair("access-token", "refresh-token");

		given(kakaoSocialAuthClient.supports(SocialProvider.KAKAO)).willReturn(true);
		given(kakaoSocialAuthClient.fetchIdentity(request)).willReturn(identity);
		given(findOrCreateSocialUserUseCase.execute(identity)).willReturn(new FindOrCreateSocialUserUseCase.Result(user, socialAccount, true));
		given(issueMemberTokensUseCase.execute(user, SocialProvider.KAKAO)).willReturn(tokenPair);

		SocialAuthDto.Response response = memberAuthService.authenticate(SocialProvider.KAKAO, request);

		assertEquals(1, response.userId());
		assertEquals("kakao", response.provider());
		assertTrue(response.isNewUser());
		assertFalse(response.profileCompleted());
		assertEquals("COMPLETE_PROFILE", response.nextAction());
		assertEquals("access-token", response.accessToken());
		assertEquals("refresh-token", response.refreshToken());
		verify(findOrCreateSocialUserUseCase).execute(identity);
	}

	@Test
	void authenticateReturnsGoHomeForCompletedExistingMember() {
		MemberAuthService memberAuthService = new MemberAuthService(
			List.of(kakaoSocialAuthClient, googleSocialAuthClient),
			findOrCreateSocialUserUseCase,
			issueMemberTokensUseCase
		);
		SocialAuthDto.Request request = new SocialAuthDto.Request(null, "google-id-token");
		SocialUserIdentity identity = new SocialUserIdentity(SocialProvider.GOOGLE, "google-user-1");
		User user = User.createPendingSocialUser("USER-002");
		setUserId(user, 2);
		user.completeProfile("moa", "moa@example.com", LocalDate.of(1997, 3, 1), true);
		SocialAccount socialAccount = SocialAccount.create(user, SocialProvider.GOOGLE, "google-user-1");
		MemberTokenPair tokenPair = new MemberTokenPair("access-token", "refresh-token");

		given(googleSocialAuthClient.supports(SocialProvider.GOOGLE)).willReturn(true);
		given(googleSocialAuthClient.fetchIdentity(request)).willReturn(identity);
		given(findOrCreateSocialUserUseCase.execute(identity)).willReturn(new FindOrCreateSocialUserUseCase.Result(user, socialAccount, false));
		given(issueMemberTokensUseCase.execute(user, SocialProvider.GOOGLE)).willReturn(tokenPair);

		SocialAuthDto.Response response = memberAuthService.authenticate(SocialProvider.GOOGLE, request);

		assertEquals(2, response.userId());
		assertEquals("google", response.provider());
		assertFalse(response.isNewUser());
		assertTrue(response.profileCompleted());
		assertEquals("GO_HOME", response.nextAction());
		assertEquals("access-token", response.accessToken());
		assertEquals("refresh-token", response.refreshToken());
		verify(findOrCreateSocialUserUseCase).execute(identity);
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

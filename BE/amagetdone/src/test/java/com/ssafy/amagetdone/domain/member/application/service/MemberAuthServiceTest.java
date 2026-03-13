package com.ssafy.amagetdone.domain.member.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.member.api.dto.SocialAuthDto;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.application.usecase.FindOrCreateSocialUserUseCase;
import com.ssafy.amagetdone.domain.member.application.usecase.IssueMemberTokensUseCase;
import com.ssafy.amagetdone.domain.member.application.usecase.ResetTestSocialUserUseCase;
import com.ssafy.amagetdone.domain.member.infra.client.SocialAuthClient;
import com.ssafy.amagetdone.domain.member.infra.client.SocialUserIdentity;
import com.ssafy.amagetdone.domain.member.model.entity.SocialAccount;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.member.support.util.MemberTokenPair;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class MemberAuthServiceTest {

	@Mock
	private SocialAuthClient kakaoSocialAuthClient;

	@Mock
	private SocialAuthClient googleSocialAuthClient;

	@Mock
	private SocialAuthClient testSocialAuthClient;

	@Mock
	private FindOrCreateSocialUserUseCase findOrCreateSocialUserUseCase;

	@Mock
	private IssueMemberTokensUseCase issueMemberTokensUseCase;

	@Mock
	private ResetTestSocialUserUseCase resetTestSocialUserUseCase;

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private PlanRepository planRepository;

	@Test
	void authenticateReturnsTempStatusWhenOnboardingFlagsAreIncomplete() {
		ListAppender<ILoggingEvent> appender = attachAppender();
		MemberAuthService memberAuthService = new MemberAuthService(
			List.of(kakaoSocialAuthClient, googleSocialAuthClient, testSocialAuthClient),
			findOrCreateSocialUserUseCase,
			issueMemberTokensUseCase,
			resetTestSocialUserUseCase,
			accountRepository,
			planRepository
		);
		SocialAuthDto.Request request = new SocialAuthDto.Request("provider-access-token", null);
		SocialUserIdentity identity = new SocialUserIdentity(SocialProvider.KAKAO, "kakao-user-1");
		User user = User.createPendingSocialUser();
		setUserId(user, 1);
		SocialAccount socialAccount = SocialAccount.create(user, SocialProvider.KAKAO, "kakao-user-1");
		MemberTokenPair tokenPair = new MemberTokenPair("access-token", "refresh-token");

		given(kakaoSocialAuthClient.supports(SocialProvider.KAKAO)).willReturn(true);
		given(kakaoSocialAuthClient.fetchIdentity(request)).willReturn(identity);
		given(findOrCreateSocialUserUseCase.execute(identity)).willReturn(new FindOrCreateSocialUserUseCase.Result(user, socialAccount, true));
		given(issueMemberTokensUseCase.execute(user, SocialProvider.KAKAO)).willReturn(tokenPair);
		given(accountRepository.existsByUserId(1)).willReturn(false);
		given(planRepository.existsByUserId(1)).willReturn(false);

		SocialAuthDto.Response response = memberAuthService.authenticate(SocialProvider.KAKAO, request);

		assertEquals("TEMP", response.status());
		assertFalse(response.account());
		assertFalse(response.plan());
		assertEquals("access-token", response.accessToken());
		assertEquals("refresh-token", response.refreshToken());
		assertTrue(containsLog(appender, "event=SOCIAL_AUTH_SUCCESS"));
		assertTrue(containsLog(appender, "event=MEMBER_LOGIN_SUCCESS"));
		verify(findOrCreateSocialUserUseCase).execute(identity);
	}

	@Test
	void authenticateReturnsIndependentFlagsForActiveMember() {
		ListAppender<ILoggingEvent> appender = attachAppender();
		MemberAuthService memberAuthService = new MemberAuthService(
			List.of(kakaoSocialAuthClient, googleSocialAuthClient, testSocialAuthClient),
			findOrCreateSocialUserUseCase,
			issueMemberTokensUseCase,
			resetTestSocialUserUseCase,
			accountRepository,
			planRepository
		);
		SocialAuthDto.Request request = new SocialAuthDto.Request(null, "google-id-token");
		SocialUserIdentity identity = new SocialUserIdentity(SocialProvider.GOOGLE, "google-user-1");
		User user = User.createPendingSocialUser();
		setUserId(user, 2);
		user.markProfileEntered("moa", "moa@example.com", LocalDate.of(1997, 3, 1), true);
		user.activate();
		SocialAccount socialAccount = SocialAccount.create(user, SocialProvider.GOOGLE, "google-user-1");
		MemberTokenPair tokenPair = new MemberTokenPair("access-token", "refresh-token");

		given(googleSocialAuthClient.supports(SocialProvider.GOOGLE)).willReturn(true);
		given(googleSocialAuthClient.fetchIdentity(request)).willReturn(identity);
		given(findOrCreateSocialUserUseCase.execute(identity)).willReturn(new FindOrCreateSocialUserUseCase.Result(user, socialAccount, false));
		given(issueMemberTokensUseCase.execute(user, SocialProvider.GOOGLE)).willReturn(tokenPair);
		given(accountRepository.existsByUserId(2)).willReturn(false);
		given(planRepository.existsByUserId(2)).willReturn(true);

		SocialAuthDto.Response response = memberAuthService.authenticate(SocialProvider.GOOGLE, request);

		assertEquals("ACTIVE", response.status());
		assertFalse(response.account());
		assertTrue(response.plan());
		assertEquals("access-token", response.accessToken());
		assertEquals("refresh-token", response.refreshToken());
		assertTrue(containsLog(appender, "event=SOCIAL_AUTH_SUCCESS"));
		assertTrue(containsLog(appender, "event=MEMBER_LOGIN_SUCCESS"));
		verify(findOrCreateSocialUserUseCase).execute(identity);
	}

	@Test
	void socialAuthResponseUsesStatusInsteadOfProfileFlag() {
		assertThrows(
			NoSuchMethodException.class,
			() -> SocialAuthDto.Response.class.getMethod("profile")
		);
	}

	@Test
	void authenticateTestUsesFixedTestIdentity() {
		ListAppender<ILoggingEvent> appender = attachAppender();
		MemberAuthService memberAuthService = new MemberAuthService(
			List.of(kakaoSocialAuthClient, googleSocialAuthClient, testSocialAuthClient),
			findOrCreateSocialUserUseCase,
			issueMemberTokensUseCase,
			resetTestSocialUserUseCase,
			accountRepository,
			planRepository
		);
		SocialUserIdentity identity = new SocialUserIdentity(SocialProvider.TEST, "test-social-user-001");
		User user = User.createPendingSocialUser();
		setUserId(user, 10);
		SocialAccount socialAccount = SocialAccount.create(user, SocialProvider.TEST, "test-social-user-001");
		MemberTokenPair tokenPair = new MemberTokenPair("access-token", "refresh-token");

		given(testSocialAuthClient.supports(SocialProvider.TEST)).willReturn(true);
		given(testSocialAuthClient.fetchIdentity(any())).willReturn(identity);
		given(findOrCreateSocialUserUseCase.execute(identity))
			.willReturn(new FindOrCreateSocialUserUseCase.Result(user, socialAccount, true));
		given(issueMemberTokensUseCase.execute(user, SocialProvider.TEST)).willReturn(tokenPair);
		given(accountRepository.existsByUserId(10)).willReturn(false);
		given(planRepository.existsByUserId(10)).willReturn(false);

		SocialAuthDto.Response response = memberAuthService.authenticateTest();

		assertEquals("TEMP", response.status());
		assertEquals("access-token", response.accessToken());
		assertTrue(containsLog(appender, "provider=TEST"));
		verify(findOrCreateSocialUserUseCase).execute(identity);
	}

	@Test
	void authenticateLogsFailureWhenSocialAuthFails() {
		ListAppender<ILoggingEvent> appender = attachAppender();
		MemberAuthService memberAuthService = new MemberAuthService(
			List.of(kakaoSocialAuthClient, googleSocialAuthClient, testSocialAuthClient),
			findOrCreateSocialUserUseCase,
			issueMemberTokensUseCase,
			resetTestSocialUserUseCase,
			accountRepository,
			planRepository
		);
		SocialAuthDto.Request request = new SocialAuthDto.Request("provider-access-token", null);

		given(kakaoSocialAuthClient.supports(SocialProvider.KAKAO)).willReturn(true);
		given(kakaoSocialAuthClient.fetchIdentity(request))
			.willThrow(new com.ssafy.amagetdone.domain.member.exception.MemberException(MemberErrorCode.INVALID_KAKAO_ACCESS_TOKEN));

		assertThrows(
			com.ssafy.amagetdone.domain.member.exception.MemberException.class,
			() -> memberAuthService.authenticate(SocialProvider.KAKAO, request)
		);

		assertTrue(containsLog(appender, "event=SOCIAL_AUTH_FAILURE"));
		assertTrue(containsLog(appender, "event=MEMBER_LOGIN_FAILURE"));
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

	private ListAppender<ILoggingEvent> attachAppender() {
		Logger logger = (Logger) LoggerFactory.getLogger(MemberAuthService.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		return appender;
	}

	private boolean containsLog(ListAppender<ILoggingEvent> appender, String text) {
		return appender.list.stream()
			.anyMatch(event -> event.getFormattedMessage().contains(text));
	}
}

package com.ssafy.amagetdone.domain.member.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.Optional;

import com.ssafy.amagetdone.config.JwtProperties;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.member.api.dto.LogoutDto;
import com.ssafy.amagetdone.domain.member.api.dto.RefreshTokenDto;
import com.ssafy.amagetdone.domain.member.application.usecase.LogoutMemberUseCase;
import com.ssafy.amagetdone.domain.member.application.usecase.ReissueMemberTokensUseCase;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.RefreshTokenStore;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;
import com.ssafy.amagetdone.domain.member.support.util.JwtRefreshTokenReader;
import com.ssafy.amagetdone.domain.member.support.util.JwtTokenIssuer;
import com.ssafy.amagetdone.domain.member.support.util.MemberTokenPair;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberTokenServiceTest {

	@Mock
	private RefreshTokenStore refreshTokenStore;

	@Mock
	private JwtRefreshTokenReader jwtRefreshTokenReader;

	@Mock
	private JwtTokenIssuer jwtTokenIssuer;

	@Mock
	private UserRepository userRepository;

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private PlanRepository planRepository;

	@Test
	void reissueRotatesRefreshTokenWhenCurrentTokenIsActive() {
		JwtProperties jwtProperties = new JwtProperties();
		jwtProperties.setRefreshTokenExpirationSeconds(1209600L);
		ReissueMemberTokensUseCase reissueMemberTokensUseCase = new ReissueMemberTokensUseCase(
			refreshTokenStore,
			jwtRefreshTokenReader,
			jwtTokenIssuer,
			jwtProperties,
			userRepository,
			accountRepository,
			planRepository
		);
		LogoutMemberUseCase logoutMemberUseCase = new LogoutMemberUseCase(refreshTokenStore, jwtRefreshTokenReader);
		MemberTokenService memberTokenService = new MemberTokenService(reissueMemberTokensUseCase, logoutMemberUseCase);
		User user = User.createPendingSocialUser();
		setUserId(user, 1);
		user.activate();

		given(jwtRefreshTokenReader.read("refresh-token"))
			.willReturn(new JwtRefreshTokenReader.RefreshTokenClaims(1, SocialProvider.KAKAO, 0L));
		given(refreshTokenStore.exists(1, "refresh-token")).willReturn(true);
		given(userRepository.findById(1)).willReturn(Optional.of(user));
		given(accountRepository.existsByUserId(1)).willReturn(true);
		given(planRepository.existsByUserId(1)).willReturn(false);
		given(jwtTokenIssuer.issue(1, SocialProvider.KAKAO, 0L))
			.willReturn(new MemberTokenPair("new-access-token", "new-refresh-token"));

		RefreshTokenDto.Response response = memberTokenService.reissue("refresh-token");

		assertEquals("ACTIVE", response.status());
		assertEquals(true, response.account());
		assertEquals(false, response.plan());
		assertEquals("new-access-token", response.accessToken());
		assertEquals("new-refresh-token", response.refreshToken());
		verify(refreshTokenStore).delete(1, "refresh-token");
		verify(refreshTokenStore).save(1, "new-refresh-token", 1209600L);
	}

	@Test
	void logoutInvalidatesAllSessionsForUser() {
		ReissueMemberTokensUseCase reissueMemberTokensUseCase = new ReissueMemberTokensUseCase(
			refreshTokenStore,
			jwtRefreshTokenReader,
			jwtTokenIssuer,
			new JwtProperties(),
			userRepository,
			accountRepository,
			planRepository
		);
		LogoutMemberUseCase logoutMemberUseCase = new LogoutMemberUseCase(refreshTokenStore, jwtRefreshTokenReader);
		MemberTokenService memberTokenService = new MemberTokenService(reissueMemberTokensUseCase, logoutMemberUseCase);

		given(jwtRefreshTokenReader.read("refresh-token"))
			.willReturn(new JwtRefreshTokenReader.RefreshTokenClaims(7, SocialProvider.GOOGLE, 2L));

		LogoutDto.Response response = memberTokenService.logout("refresh-token");

		assertEquals(true, response.success());
		verify(refreshTokenStore).invalidateAll(7);
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

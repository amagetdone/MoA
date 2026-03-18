package com.ssafy.amagetdone.domain.member.application.service;

import java.util.List;

import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.member.api.dto.SocialAuthDto;
import com.ssafy.amagetdone.domain.member.application.usecase.EnsureFinMemberLinkedUseCase;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.application.usecase.FindOrCreateSocialUserUseCase;
import com.ssafy.amagetdone.domain.member.application.usecase.IssueMemberTokensUseCase;
import com.ssafy.amagetdone.domain.member.application.usecase.ResetTestSocialUserUseCase;
import com.ssafy.amagetdone.domain.member.infra.client.SocialAuthClient;
import com.ssafy.amagetdone.domain.member.infra.client.SocialUserIdentity;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.member.support.log.MemberLogHelper;
import com.ssafy.amagetdone.domain.member.support.util.MemberTokenPair;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberAuthService {

	private static final Logger log = LoggerFactory.getLogger(MemberAuthService.class);

	private final List<SocialAuthClient> socialAuthClients;
	private final FindOrCreateSocialUserUseCase findOrCreateSocialUserUseCase;
	private final IssueMemberTokensUseCase issueMemberTokensUseCase;
	private final ResetTestSocialUserUseCase resetTestSocialUserUseCase;
	private final EnsureFinMemberLinkedUseCase ensureFinMemberLinkedUseCase;
	private final AccountRepository accountRepository;
	private final PlanRepository planRepository;

	public SocialAuthDto.Response authenticate(SocialProvider provider, SocialAuthDto.Request request) {
		return authenticateInternal(provider, request);
	}

	public SocialAuthDto.Response authenticateTest() {
		return authenticateInternal(SocialProvider.TEST, new SocialAuthDto.Request(null, null));
	}

	public void resetTestUser() {
		String requestId = MemberLogHelper.requestId();
		try {
			resetTestSocialUserUseCase.execute();
			log.info("event=TEST_SOCIAL_RESET_SUCCESS requestId={}", requestId);
		} catch (RuntimeException exception) {
			log.warn(
				"event=TEST_SOCIAL_RESET_FAILURE requestId={} errorType={}",
				requestId,
				exception.getClass().getSimpleName()
			);
			throw exception;
		}
	}

	private SocialAuthDto.Response authenticateInternal(SocialProvider provider, SocialAuthDto.Request request) {
		String requestId = MemberLogHelper.requestId();
		boolean socialAuthSucceeded = false;
		try {
			SocialAuthClient client = socialAuthClients.stream()
				.filter(candidate -> candidate.supports(provider))
				.findFirst()
				.orElseThrow(() -> new MemberException(MemberErrorCode.UNSUPPORTED_SOCIAL_PROVIDER));

			SocialUserIdentity identity = client.fetchIdentity(request);
			socialAuthSucceeded = true;
			log.info(
				"event=SOCIAL_AUTH_SUCCESS requestId={} provider={} providerUid={}",
				requestId,
				provider.name(),
				identity.providerUid()
			);

			FindOrCreateSocialUserUseCase.Result result = findOrCreateSocialUserUseCase.execute(identity);
			User user = result.user();
			linkTestFinancialMemberIfNeeded(provider, user);
			MemberTokenPair tokenPair = issueMemberTokensUseCase.execute(user, provider);
			boolean hasAccount = accountRepository.existsLinkedAccountByUserId(user.getId());
			boolean hasPlan = planRepository.existsByUserId(user.getId());

			log.info(
				"event=MEMBER_LOGIN_SUCCESS requestId={} provider={} localUserId={} isNewUser={} userStatus={} account={} plan={}",
				requestId,
				provider.name(),
				user.getId(),
				result.isNewUser(),
				user.getStatus().name(),
				hasAccount,
				hasPlan
			);

			return new SocialAuthDto.Response(
				user.getStatus().name(),
				hasAccount,
				hasPlan,
				tokenPair.accessToken(),
				tokenPair.refreshToken()
			);
		} catch (RuntimeException exception) {
			if (!socialAuthSucceeded) {
				log.warn(
					"event=SOCIAL_AUTH_FAILURE requestId={} provider={} errorType={}",
					requestId,
					provider.name(),
					exception.getClass().getSimpleName()
				);
			}
			log.warn(
				"event=MEMBER_LOGIN_FAILURE requestId={} provider={} errorType={}",
				requestId,
				provider.name(),
				exception.getClass().getSimpleName()
			);
			throw exception;
		}
	}

	private void linkTestFinancialMemberIfNeeded(SocialProvider provider, User user) {
		if (provider != SocialProvider.TEST) {
			return;
		}
		String userKey = ensureFinMemberLinkedUseCase.execute(user.getId(), user.getEmail());
		user.updateUserKey(userKey);
	}
}

package com.ssafy.amagetdone.domain.member.application.service;

import java.util.List;

import com.ssafy.amagetdone.domain.asset.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.member.api.dto.SocialAuthDto;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.application.usecase.FindOrCreateSocialUserUseCase;
import com.ssafy.amagetdone.domain.member.application.usecase.IssueMemberTokensUseCase;
import com.ssafy.amagetdone.domain.member.infra.client.SocialAuthClient;
import com.ssafy.amagetdone.domain.member.infra.client.SocialUserIdentity;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.member.support.util.MemberTokenPair;

import org.springframework.stereotype.Service;

@Service
public class MemberAuthService {

	private final List<SocialAuthClient> socialAuthClients;
	private final FindOrCreateSocialUserUseCase findOrCreateSocialUserUseCase;
	private final IssueMemberTokensUseCase issueMemberTokensUseCase;
	private final AccountRepository accountRepository;
	private final PlanRepository planRepository;

	public MemberAuthService(
		List<SocialAuthClient> socialAuthClients,
		FindOrCreateSocialUserUseCase findOrCreateSocialUserUseCase,
		IssueMemberTokensUseCase issueMemberTokensUseCase,
		AccountRepository accountRepository,
		PlanRepository planRepository
	) {
		this.socialAuthClients = socialAuthClients;
		this.findOrCreateSocialUserUseCase = findOrCreateSocialUserUseCase;
		this.issueMemberTokensUseCase = issueMemberTokensUseCase;
		this.accountRepository = accountRepository;
		this.planRepository = planRepository;
	}

	public SocialAuthDto.Response authenticate(SocialProvider provider, SocialAuthDto.Request request) {
		SocialAuthClient client = socialAuthClients.stream()
			.filter(candidate -> candidate.supports(provider))
			.findFirst()
			.orElseThrow(() -> new MemberException(MemberErrorCode.UNSUPPORTED_SOCIAL_PROVIDER));

		SocialUserIdentity identity = client.fetchIdentity(request);
		FindOrCreateSocialUserUseCase.Result result = findOrCreateSocialUserUseCase.execute(identity);
		User user = result.user();
		MemberTokenPair tokenPair = issueMemberTokensUseCase.execute(user, provider);
		boolean profileCompleted = user.isProfileCompleted();
		boolean hasAccount = accountRepository.existsByUserId(user.getId());
		boolean hasPlan = planRepository.existsByUserId(user.getId());

		return new SocialAuthDto.Response(
			user.getStatus().name(),
			profileCompleted,
			hasAccount,
			hasPlan,
			tokenPair.accessToken(),
			tokenPair.refreshToken()
		);
	}
}

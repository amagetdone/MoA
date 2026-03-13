package com.ssafy.amagetdone.domain.member.application.usecase;

import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.member.infra.client.MockSocialAuthClient;
import com.ssafy.amagetdone.domain.member.model.repository.RefreshTokenStore;
import com.ssafy.amagetdone.domain.member.model.repository.SocialAccountRepository;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ResetTestSocialUserUseCase {

	private final SocialAccountRepository socialAccountRepository;
	private final AccountRepository accountRepository;
	private final PlanRepository planRepository;
	private final PlanWeekAllocationRepository planWeekAllocationRepository;
	private final RefreshTokenStore refreshTokenStore;

	public ResetTestSocialUserUseCase(
		SocialAccountRepository socialAccountRepository,
		AccountRepository accountRepository,
		PlanRepository planRepository,
		PlanWeekAllocationRepository planWeekAllocationRepository,
		RefreshTokenStore refreshTokenStore
	) {
		this.socialAccountRepository = socialAccountRepository;
		this.accountRepository = accountRepository;
		this.planRepository = planRepository;
		this.planWeekAllocationRepository = planWeekAllocationRepository;
		this.refreshTokenStore = refreshTokenStore;
	}

	@Transactional
	public void execute() {
		socialAccountRepository.findByProviderAndProviderUid(SocialProvider.TEST, MockSocialAuthClient.TEST_PROVIDER_UID)
			.ifPresent(socialAccount -> {
				Integer userId = socialAccount.getUser().getId();
				planWeekAllocationRepository.deleteByPlanUserId(userId);
				planRepository.deleteByUserId(userId);
				accountRepository.deleteByUserId(userId);
				socialAccount.getUser().resetToPending();
				refreshTokenStore.invalidateAll(userId);
			});
	}
}

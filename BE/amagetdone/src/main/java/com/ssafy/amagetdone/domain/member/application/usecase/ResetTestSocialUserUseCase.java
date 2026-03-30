package com.ssafy.amagetdone.domain.member.application.usecase;

import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.account.model.repository.CardRepository;
import com.ssafy.amagetdone.domain.accountverification.model.repository.AccountVerificationRepository;
import com.ssafy.amagetdone.domain.member.model.repository.RefreshTokenStore;
import com.ssafy.amagetdone.domain.member.model.repository.SocialAccountRepository;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ResetTestSocialUserUseCase {

	private final SocialAccountRepository socialAccountRepository;
	private final ResetUserFlowStateUseCase resetUserFlowStateUseCase;
	private final AccountRepository accountRepository;
	private final CardRepository cardRepository;
	private final AccountVerificationRepository accountVerificationRepository;
	private final RefreshTokenStore refreshTokenStore;

	public ResetTestSocialUserUseCase(
		SocialAccountRepository socialAccountRepository,
		ResetUserFlowStateUseCase resetUserFlowStateUseCase,
		AccountRepository accountRepository,
		CardRepository cardRepository,
		AccountVerificationRepository accountVerificationRepository,
		RefreshTokenStore refreshTokenStore
	) {
		this.socialAccountRepository = socialAccountRepository;
		this.resetUserFlowStateUseCase = resetUserFlowStateUseCase;
		this.accountRepository = accountRepository;
		this.cardRepository = cardRepository;
		this.accountVerificationRepository = accountVerificationRepository;
		this.refreshTokenStore = refreshTokenStore;
	}

	@Transactional
	public void execute(Integer userId) {
		socialAccountRepository.findByUser_IdAndProvider(userId, SocialProvider.TEST)
			.ifPresent(socialAccount -> {
				Integer targetUserId = socialAccount.getUser().getId();

				resetUserFlowStateUseCase.execute(targetUserId);

				// 15. Account verifications (FK → User)
				accountVerificationRepository.deleteAllByUserId(targetUserId);

				// 16. Cards (FK → Account)
				cardRepository.deleteAllByUserId(targetUserId);

				// 17. Accounts (FK → User)
				accountRepository.deleteByUserId(targetUserId);

				// 18. Reset user status to PENDING
				socialAccount.getUser().resetToPending();

				// 19. Invalidate refresh tokens
				refreshTokenStore.invalidateAll(targetUserId);
			});
	}
}

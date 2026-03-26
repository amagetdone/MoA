package com.ssafy.amagetdone.domain.member.application.usecase;

import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.account.model.repository.CardRepository;
import com.ssafy.amagetdone.domain.account.model.repository.PocketRepository;
import com.ssafy.amagetdone.domain.accountverification.model.repository.AccountVerificationRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetMonthRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetRolloverRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseMonthRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekCategoryRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialWeekAllocationRepository;
import com.ssafy.amagetdone.domain.member.model.repository.RefreshTokenStore;
import com.ssafy.amagetdone.domain.member.model.repository.SocialAccountRepository;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.AccountTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.CardTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisMetadataRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisResultRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ResetTestSocialUserUseCase {

	private final SocialAccountRepository socialAccountRepository;
	private final AccountRepository accountRepository;
	private final CardRepository cardRepository;
	private final PocketRepository pocketRepository;
	private final AccountVerificationRepository accountVerificationRepository;
	private final PlanRepository planRepository;
	private final PlanWeekAllocationRepository planWeekAllocationRepository;
	private final BudgetAdjustmentRepository budgetAdjustmentRepository;
	private final BudgetRolloverRepository budgetRolloverRepository;
	private final BudgetWeekRepository budgetWeekRepository;
	private final BudgetMonthRepository budgetMonthRepository;
	private final EssentialWeekAllocationRepository essentialWeekAllocationRepository;
	private final EssentialExpenseSnapshotWeekCategoryRepository essentialExpenseSnapshotWeekCategoryRepository;
	private final EssentialExpenseSnapshotWeekRepository essentialExpenseSnapshotWeekRepository;
	private final EssentialExpenseMonthRepository essentialExpenseMonthRepository;
	private final TransactionAnalysisResultRepository transactionAnalysisResultRepository;
	private final AccountTransactionOverrideRepository accountTransactionOverrideRepository;
	private final CardTransactionOverrideRepository cardTransactionOverrideRepository;
	private final TransactionAnalysisMetadataRepository transactionAnalysisMetadataRepository;
	private final RefreshTokenStore refreshTokenStore;

	public ResetTestSocialUserUseCase(
		SocialAccountRepository socialAccountRepository,
		AccountRepository accountRepository,
		CardRepository cardRepository,
		PocketRepository pocketRepository,
		AccountVerificationRepository accountVerificationRepository,
		PlanRepository planRepository,
		PlanWeekAllocationRepository planWeekAllocationRepository,
		BudgetAdjustmentRepository budgetAdjustmentRepository,
		BudgetRolloverRepository budgetRolloverRepository,
		BudgetWeekRepository budgetWeekRepository,
		BudgetMonthRepository budgetMonthRepository,
		EssentialWeekAllocationRepository essentialWeekAllocationRepository,
		EssentialExpenseSnapshotWeekCategoryRepository essentialExpenseSnapshotWeekCategoryRepository,
		EssentialExpenseSnapshotWeekRepository essentialExpenseSnapshotWeekRepository,
		EssentialExpenseMonthRepository essentialExpenseMonthRepository,
		TransactionAnalysisResultRepository transactionAnalysisResultRepository,
		AccountTransactionOverrideRepository accountTransactionOverrideRepository,
		CardTransactionOverrideRepository cardTransactionOverrideRepository,
		TransactionAnalysisMetadataRepository transactionAnalysisMetadataRepository,
		RefreshTokenStore refreshTokenStore
	) {
		this.socialAccountRepository = socialAccountRepository;
		this.accountRepository = accountRepository;
		this.cardRepository = cardRepository;
		this.pocketRepository = pocketRepository;
		this.accountVerificationRepository = accountVerificationRepository;
		this.planRepository = planRepository;
		this.planWeekAllocationRepository = planWeekAllocationRepository;
		this.budgetAdjustmentRepository = budgetAdjustmentRepository;
		this.budgetRolloverRepository = budgetRolloverRepository;
		this.budgetWeekRepository = budgetWeekRepository;
		this.budgetMonthRepository = budgetMonthRepository;
		this.essentialWeekAllocationRepository = essentialWeekAllocationRepository;
		this.essentialExpenseSnapshotWeekCategoryRepository = essentialExpenseSnapshotWeekCategoryRepository;
		this.essentialExpenseSnapshotWeekRepository = essentialExpenseSnapshotWeekRepository;
		this.essentialExpenseMonthRepository = essentialExpenseMonthRepository;
		this.transactionAnalysisResultRepository = transactionAnalysisResultRepository;
		this.accountTransactionOverrideRepository = accountTransactionOverrideRepository;
		this.cardTransactionOverrideRepository = cardTransactionOverrideRepository;
		this.transactionAnalysisMetadataRepository = transactionAnalysisMetadataRepository;
		this.refreshTokenStore = refreshTokenStore;
	}

	@Transactional
	public void execute(Integer userId) {
		socialAccountRepository.findByUser_IdAndProvider(userId, SocialProvider.TEST)
			.ifPresent(socialAccount -> {
				Integer targetUserId = socialAccount.getUser().getId();

				// 1. Transaction analysis results (FK → TransactionOverride)
				transactionAnalysisResultRepository.deleteAllByUserId(targetUserId);

				// 2. Transaction overrides (FK → Account/Card, inherits TransactionOverride)
				accountTransactionOverrideRepository.deleteAllByUserId(targetUserId);
				cardTransactionOverrideRepository.deleteAllByUserId(targetUserId);

				// 3. Transaction analysis metadata (FK → User)
				transactionAnalysisMetadataRepository.deleteAllByUserId(targetUserId);

				// 4. Budget adjustments (FK → BudgetWeek, Plan)
				budgetAdjustmentRepository.deleteAllByUserId(targetUserId);

				// 5. Budget rollovers (FK → BudgetWeek)
				budgetRolloverRepository.deleteAllByUserId(targetUserId);

				// 6. Essential week allocations (FK → BudgetWeek, EssentialExpenseSnapshotWeek)
				essentialWeekAllocationRepository.deleteAllByUserId(targetUserId);

				// 7. Essential expense snapshot week categories (FK → EssentialExpenseSnapshotWeek)
				essentialExpenseSnapshotWeekCategoryRepository.deleteAllByUserId(targetUserId);

				// 8. Essential expense snapshot weeks (FK → User)
				essentialExpenseSnapshotWeekRepository.deleteAllByUserId(targetUserId);

				// 9. Essential expense months (FK → BudgetMonth)
				essentialExpenseMonthRepository.deleteAllByUserId(targetUserId);

				// 10. Plan week allocations (FK → Plan, BudgetWeek)
				planWeekAllocationRepository.deleteAllByUserId(targetUserId);

				// 11. Pockets (FK → Account, BudgetWeek)
				pocketRepository.deleteAllByUserId(targetUserId);

				// 12. Plans (FK → User)
				planRepository.deleteAllByUserId(targetUserId);

				// 13. Budget weeks (FK → User)
				budgetWeekRepository.deleteAllByUserId(targetUserId);

				// 14. Budget months (FK → User)
				budgetMonthRepository.deleteAllByUserId(targetUserId);

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

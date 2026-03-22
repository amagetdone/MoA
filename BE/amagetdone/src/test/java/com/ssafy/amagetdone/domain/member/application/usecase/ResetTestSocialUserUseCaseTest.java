package com.ssafy.amagetdone.domain.member.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;

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
import com.ssafy.amagetdone.domain.member.model.entity.SocialAccount;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.RefreshTokenStore;
import com.ssafy.amagetdone.domain.member.model.repository.SocialAccountRepository;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.AccountTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.CardTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisMetadataRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisResultRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResetTestSocialUserUseCaseTest {

	@Mock
	private SocialAccountRepository socialAccountRepository;

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private CardRepository cardRepository;

	@Mock
	private PocketRepository pocketRepository;

	@Mock
	private AccountVerificationRepository accountVerificationRepository;

	@Mock
	private PlanRepository planRepository;

	@Mock
	private PlanWeekAllocationRepository planWeekAllocationRepository;

	@Mock
	private BudgetAdjustmentRepository budgetAdjustmentRepository;

	@Mock
	private BudgetRolloverRepository budgetRolloverRepository;

	@Mock
	private BudgetWeekRepository budgetWeekRepository;

	@Mock
	private BudgetMonthRepository budgetMonthRepository;

	@Mock
	private EssentialWeekAllocationRepository essentialWeekAllocationRepository;

	@Mock
	private EssentialExpenseSnapshotWeekCategoryRepository essentialExpenseSnapshotWeekCategoryRepository;

	@Mock
	private EssentialExpenseSnapshotWeekRepository essentialExpenseSnapshotWeekRepository;

	@Mock
	private EssentialExpenseMonthRepository essentialExpenseMonthRepository;

	@Mock
	private TransactionAnalysisResultRepository transactionAnalysisResultRepository;

	@Mock
	private AccountTransactionOverrideRepository accountTransactionOverrideRepository;

	@Mock
	private CardTransactionOverrideRepository cardTransactionOverrideRepository;

	@Mock
	private TransactionAnalysisMetadataRepository transactionAnalysisMetadataRepository;

	@Mock
	private RefreshTokenStore refreshTokenStore;

	private ResetTestSocialUserUseCase createUseCase() {
		return new ResetTestSocialUserUseCase(
			socialAccountRepository,
			accountRepository,
			cardRepository,
			pocketRepository,
			accountVerificationRepository,
			planRepository,
			planWeekAllocationRepository,
			budgetAdjustmentRepository,
			budgetRolloverRepository,
			budgetWeekRepository,
			budgetMonthRepository,
			essentialWeekAllocationRepository,
			essentialExpenseSnapshotWeekCategoryRepository,
			essentialExpenseSnapshotWeekRepository,
			essentialExpenseMonthRepository,
			transactionAnalysisResultRepository,
			accountTransactionOverrideRepository,
			cardTransactionOverrideRepository,
			transactionAnalysisMetadataRepository,
			refreshTokenStore
		);
	}

	@Test
	void executeResetsTestUserStateAndInvalidatesTokens() {
		ResetTestSocialUserUseCase useCase = createUseCase();
		User user = User.createPendingSocialUser();
		setUserId(user, 1);
		user.markProfileEntered("moa", "moa@example.com", LocalDate.of(1997, 3, 1), true);
		user.updateUserKey("user-key");
		user.activate();
		SocialAccount socialAccount = SocialAccount.create(user, SocialProvider.TEST, "test-social-user-001");

		given(socialAccountRepository.findByProviderAndProviderUid(SocialProvider.TEST, "test-social-user-001"))
			.willReturn(Optional.of(socialAccount));

		useCase.execute();

		verify(transactionAnalysisResultRepository).deleteAllByUserId(1);
		verify(accountTransactionOverrideRepository).deleteAllByUserId(1);
		verify(cardTransactionOverrideRepository).deleteAllByUserId(1);
		verify(transactionAnalysisMetadataRepository).deleteAllByUserId(1);
		verify(budgetAdjustmentRepository).deleteAllByUserId(1);
		verify(budgetRolloverRepository).deleteAllByUserId(1);
		verify(essentialWeekAllocationRepository).deleteAllByUserId(1);
		verify(essentialExpenseSnapshotWeekCategoryRepository).deleteAllByUserId(1);
		verify(essentialExpenseSnapshotWeekRepository).deleteAllByUserId(1);
		verify(essentialExpenseMonthRepository).deleteAllByUserId(1);
		verify(planWeekAllocationRepository).deleteAllByUserId(1);
		verify(pocketRepository).deleteAllByUserId(1);
		verify(planRepository).deleteAllByUserId(1);
		verify(budgetWeekRepository).deleteAllByUserId(1);
		verify(budgetMonthRepository).deleteAllByUserId(1);
		verify(accountVerificationRepository).deleteAllByUserId(1);
		verify(cardRepository).deleteAllByUserId(1);
		verify(accountRepository).deleteByUserId(1);
		verify(refreshTokenStore).invalidateAll(1);
		assertEquals("TEMP", user.getStatus().name());
		assertNull(user.getEmail());
		assertNull(user.getUserKey());
	}

	@Test
	void executeIsIdempotentWhenTestUserDoesNotExist() {
		ResetTestSocialUserUseCase useCase = createUseCase();
		given(socialAccountRepository.findByProviderAndProviderUid(SocialProvider.TEST, "test-social-user-001"))
			.willReturn(Optional.empty());

		useCase.execute();

		verifyNoInteractions(accountRepository, planRepository, planWeekAllocationRepository, refreshTokenStore);
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

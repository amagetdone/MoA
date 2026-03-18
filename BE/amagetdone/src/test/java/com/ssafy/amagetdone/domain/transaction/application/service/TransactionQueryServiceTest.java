package com.ssafy.amagetdone.domain.transaction.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Card;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.entity.Merchant;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekCategoryRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.vo.UserStatus;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.application.usecase.SyncLatestTransactionsUseCase;
import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisMetadata;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisResult;
import com.ssafy.amagetdone.domain.transaction.model.repository.AccountTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.CardTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisMetadataRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisResultRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionQueryServiceTest {

	@Mock
	private AccountTransactionOverrideRepository accountTransactionOverrideRepository;

	@Mock
	private CardTransactionOverrideRepository cardTransactionOverrideRepository;

	@Mock
	private TransactionAnalysisResultRepository transactionAnalysisResultRepository;

	@Mock
	private TransactionAnalysisMetadataRepository transactionAnalysisMetadataRepository;

	@Mock
	private EssentialExpenseSnapshotWeekRepository essentialExpenseSnapshotWeekRepository;

	@Mock
	private EssentialExpenseSnapshotWeekCategoryRepository essentialExpenseSnapshotWeekCategoryRepository;

	@Mock
	private SyncLatestTransactionsUseCase syncLatestTransactionsUseCase;

	@Test
	void getOnboardingAnalysisResultUsesLatestStoredMetadataWindow() {
		TransactionQueryService queryService = new TransactionQueryService(
			accountTransactionOverrideRepository,
			cardTransactionOverrideRepository,
			transactionAnalysisResultRepository,
			transactionAnalysisMetadataRepository,
			essentialExpenseSnapshotWeekRepository,
			essentialExpenseSnapshotWeekCategoryRepository,
			syncLatestTransactionsUseCase
		);

		User user = User.builder()
			.id(1)
			.email("tester@example.com")
			.nickname("tester")
			.status(UserStatus.ACTIVE)
			.build();
		Account account = Account.create(user, "111-222-333", "type-1", "main", "deposit", "001", "bank", "KRW");
		Card card = Card.builder()
			.cardUniqueNo(2001L)
			.withdrawalAccount(account)
			.cvc("123")
			.cardNo("1234-5678-9012-3456")
			.name("card")
			.issuerCode("001")
			.cardIssuerName("issuer")
			.cardExpiryDate(LocalDateTime.of(2027, 12, 31, 0, 0))
			.cardDescription("card")
			.withdrawalDate("25")
			.baselinePerformance(0L)
			.maxBenefitLimit(0L)
			.build();
		Merchant merchant = Merchant.builder().id(1).name("Store").build();
		Category housing = Category.create(10, "Housing");
		Category food = Category.create(30, "Food");
		Category transport = Category.create(40, "Transport");

		AccountTransactionOverride decIncome = accountTransaction(1, account, housing, LocalDateTime.of(2025, 12, 5, 9, 0), 3_000_000L, 1, "Salary");
		AccountTransactionOverride janIncome = accountTransaction(2, account, housing, LocalDateTime.of(2026, 1, 5, 9, 0), 3_000_000L, 1, "Salary");
		AccountTransactionOverride febIncome = accountTransaction(3, account, housing, LocalDateTime.of(2026, 2, 5, 9, 0), 3_000_000L, 1, "Salary");
		AccountTransactionOverride decHousing = accountTransaction(4, account, housing, LocalDateTime.of(2025, 12, 10, 8, 0), 800_000L, 2, "Rent");
		AccountTransactionOverride janHousing = accountTransaction(5, account, housing, LocalDateTime.of(2026, 1, 10, 8, 0), 800_000L, 2, "Rent");
		AccountTransactionOverride febHousing = accountTransaction(6, account, housing, LocalDateTime.of(2026, 2, 10, 8, 0), 800_000L, 2, "Rent");
		CardTransactionOverride decFood = cardTransaction(7, card, merchant, food, LocalDateTime.of(2025, 12, 20, 12, 0), 600_000L, "Food");
		CardTransactionOverride janFood = cardTransaction(8, card, merchant, food, LocalDateTime.of(2026, 1, 20, 12, 0), 600_000L, "Food");
		CardTransactionOverride febFood = cardTransaction(9, card, merchant, food, LocalDateTime.of(2026, 2, 20, 12, 0), 600_000L, "Food");
		CardTransactionOverride marchTransport = cardTransaction(10, card, merchant, transport, LocalDateTime.of(2026, 3, 25, 12, 0), 600_000L, "Transport");

		TransactionAnalysisMetadata latestMetadata = TransactionAnalysisMetadata.builder()
			.id(1)
			.user(user)
			.requestedMonths(3)
			.windowStartAt(LocalDateTime.of(2025, 12, 1, 0, 0))
			.windowEndAt(LocalDateTime.of(2026, 2, 28, 23, 59, 59, 999_999_999))
			.transactionCount(9)
			.latestTransactionAt(LocalDateTime.of(2026, 2, 20, 12, 0))
			.analyzedAt(LocalDateTime.of(2026, 3, 1, 0, 0))
			.build();

		given(transactionAnalysisMetadataRepository.findTopByUser_IdOrderByAnalyzedAtDesc(1))
			.willReturn(Optional.of(latestMetadata));
		given(accountTransactionOverrideRepository
			.findAllByAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(any(), any(), any()))
			.willReturn(List.of(
				febHousing, febIncome, janHousing, janIncome, decHousing, decIncome
			));
		given(cardTransactionOverrideRepository
			.findAllByCard_WithdrawalAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(any(), any(), any()))
			.willReturn(List.of(febFood, janFood, decFood));
		given(transactionAnalysisResultRepository.findAllByTransaction_IdInAndAnalyzedAt(anyList(), any()))
			.willReturn(List.of(
				includedResult(decHousing, housing, ExpenseScope.FIXED),
				includedResult(janHousing, housing, ExpenseScope.FIXED),
				includedResult(febHousing, housing, ExpenseScope.FIXED),
				includedResult(decFood, food, ExpenseScope.VARIABLE),
				includedResult(janFood, food, ExpenseScope.VARIABLE),
				includedResult(febFood, food, ExpenseScope.VARIABLE),
				includedResult(marchTransport, transport, ExpenseScope.VARIABLE)
			));

		TransactionAnalysisDto.OnboardingResult result = queryService.getOnboardingAnalysisResult(1);

		assertEquals("최근 3개월 소비 분석을 완료했어요.", result.summaryText());
		assertEquals(3_000_000L, result.averageIncome());
		assertEquals(1_400_000L, result.averageExpense());
		assertEquals(1_600_000L, result.remainingAmount());
		assertEquals(57.14, result.fixedExpenseRatio());
		assertEquals(42.86, result.variableExpenseRatio());
		assertEquals("현재 소비 흐름은 안정적이에요.", result.insightMessage());
		assertEquals(2, result.categoryExpenses().size());
		assertEquals("Housing", result.categoryExpenses().get(0).categoryName());
		assertEquals(2_400_000L, result.categoryExpenses().get(0).amount());
		assertEquals("Food", result.categoryExpenses().get(1).categoryName());
		assertEquals(1_800_000L, result.categoryExpenses().get(1).amount());
	}

	private AccountTransactionOverride accountTransaction(
		int id,
		Account account,
		Category category,
		LocalDateTime occurredAt,
		long amount,
		int transactionType,
		String summary
	) {
		return AccountTransactionOverride.builder()
			.id(id)
			.account(account)
			.category(category)
			.transactionDateTime(occurredAt)
			.transactionBalance(amount)
			.transactionMemo(summary)
			.transactionUniqueNo((long) id)
			.transactionType(transactionType)
			.transactionTypeName(transactionType == 1 ? "IN" : "OUT")
			.transactionAfterBalance(0L)
			.transactionSummary(summary)
			.build();
	}

	private CardTransactionOverride cardTransaction(
		int id,
		Card card,
		Merchant merchant,
		Category category,
		LocalDateTime occurredAt,
		long amount,
		String memo
	) {
		return CardTransactionOverride.builder()
			.id(id)
			.card(card)
			.merchant(merchant)
			.category(category)
			.transactionDateTime(occurredAt)
			.transactionBalance(amount)
			.transactionMemo(memo)
			.transactionUniqueNo((long) id)
			.cardStatus(true)
			.billStatementsYn(false)
			.billStatementsStatus("PENDING")
			.build();
	}

	private TransactionAnalysisResult includedResult(
		com.ssafy.amagetdone.domain.transaction.model.entity.TransactionOverride transaction,
		Category category,
		ExpenseScope expenseScope
	) {
		return TransactionAnalysisResult.builder()
			.id(transaction.getId())
			.transaction(transaction)
			.category(category)
			.analysisIncluded(true)
			.expenseScope(expenseScope)
			.analyzedAt(LocalDateTime.of(2026, 3, 1, 0, 0))
			.build();
	}
}

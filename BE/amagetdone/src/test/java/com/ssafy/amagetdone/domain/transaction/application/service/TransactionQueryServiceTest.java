package com.ssafy.amagetdone.domain.transaction.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Card;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.entity.Merchant;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekCategoryRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.vo.UserStatus;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisJobDto;
import com.ssafy.amagetdone.domain.transaction.application.usecase.SyncLatestTransactionsUseCase;
import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisResult;
import com.ssafy.amagetdone.domain.transaction.model.repository.AccountTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.CardTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisResultRepository;

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
	private EssentialExpenseSnapshotWeekRepository essentialExpenseSnapshotWeekRepository;

	@Mock
	private EssentialExpenseSnapshotWeekCategoryRepository essentialExpenseSnapshotWeekCategoryRepository;

	@Mock
	private SyncLatestTransactionsUseCase syncLatestTransactionsUseCase;

	@Test
	void getOnboardingAnalysisResultAggregatesRecentThreeMonths() {
		TransactionQueryService queryService = new TransactionQueryService(
			accountTransactionOverrideRepository,
			cardTransactionOverrideRepository,
			transactionAnalysisResultRepository,
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
		Account account = Account.create(user, "111-222-333", "type-1", "주거래 통장", "입출금", "001", "모아은행", "KRW");
		Card card = Card.builder()
			.cardUniqueNo(2001L)
			.withdrawalAccount(account)
			.cvc("123")
			.cardNo("1234-5678-9012-3456")
			.name("모아카드")
			.issuerCode("001")
			.cardIssuerName("모아은행")
			.cardExpiryDate(LocalDateTime.of(2027, 12, 31, 0, 0))
			.cardDescription("테스트 카드")
			.withdrawalDate("25")
			.baselinePerformance(0L)
			.maxBenefitLimit(0L)
			.build();
		Merchant merchant = Merchant.builder().id(1).name("가맹점").build();
		Category housing = Category.create(10, "주거비");
		Category telecom = Category.create(20, "통신비");
		Category food = Category.create(30, "식비");
		Category transport = Category.create(40, "교통비");

		AccountTransactionOverride janIncome = accountTransaction(1, account, housing, LocalDateTime.of(2026, 1, 5, 9, 0), 3_000_000L, 1, "급여");
		AccountTransactionOverride febIncome = accountTransaction(2, account, housing, LocalDateTime.of(2026, 2, 5, 9, 0), 3_000_000L, 1, "급여");
		AccountTransactionOverride marIncome = accountTransaction(3, account, housing, LocalDateTime.of(2026, 3, 5, 9, 0), 3_000_000L, 1, "급여");
		AccountTransactionOverride janHousing = accountTransaction(4, account, housing, LocalDateTime.of(2026, 1, 10, 8, 0), 800_000L, 2, "월세");
		AccountTransactionOverride febHousing = accountTransaction(5, account, housing, LocalDateTime.of(2026, 2, 10, 8, 0), 800_000L, 2, "월세");
		AccountTransactionOverride marHousing = accountTransaction(6, account, housing, LocalDateTime.of(2026, 3, 10, 8, 0), 800_000L, 2, "월세");
		AccountTransactionOverride janTelecom = accountTransaction(7, account, telecom, LocalDateTime.of(2026, 1, 15, 8, 0), 600_000L, 2, "통신비");
		AccountTransactionOverride febTelecom = accountTransaction(8, account, telecom, LocalDateTime.of(2026, 2, 15, 8, 0), 600_000L, 2, "통신비");
		CardTransactionOverride janFood = cardTransaction(9, card, merchant, food, LocalDateTime.of(2026, 1, 20, 12, 0), 600_000L, "식비");
		CardTransactionOverride febFood = cardTransaction(10, card, merchant, food, LocalDateTime.of(2026, 2, 20, 12, 0), 600_000L, "식비");
		CardTransactionOverride marFood = cardTransaction(11, card, merchant, food, LocalDateTime.of(2026, 3, 20, 12, 0), 600_000L, "식비");
		CardTransactionOverride marTransport = cardTransaction(12, card, merchant, transport, LocalDateTime.of(2026, 3, 25, 12, 0), 600_000L, "교통비");

		given(accountTransactionOverrideRepository
			.findAllByAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(any(), any(), any()))
			.willReturn(List.of(
				marHousing, marIncome, febHousing, febIncome, janHousing, janIncome, febTelecom, janTelecom
			));
		given(cardTransactionOverrideRepository
			.findAllByCard_WithdrawalAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(any(), any(), any()))
			.willReturn(List.of(marTransport, marFood, febFood, janFood));
		given(transactionAnalysisResultRepository.findAllByTransaction_IdIn(anyList()))
			.willReturn(List.of(
				includedResult(janHousing, housing, ExpenseScope.FIXED),
				includedResult(febHousing, housing, ExpenseScope.FIXED),
				includedResult(marHousing, housing, ExpenseScope.FIXED),
				includedResult(janTelecom, telecom, ExpenseScope.FIXED),
				includedResult(febTelecom, telecom, ExpenseScope.FIXED),
				includedResult(janFood, food, ExpenseScope.VARIABLE),
				includedResult(febFood, food, ExpenseScope.VARIABLE),
				includedResult(marFood, food, ExpenseScope.VARIABLE),
				includedResult(marTransport, transport, ExpenseScope.VARIABLE)
			));

		TransactionAnalysisJobDto.OnboardingResult result = queryService.getOnboardingAnalysisResult(
			1,
			LocalDateTime.of(2026, 3, 31, 23, 59, 59)
		);

		assertEquals("3개월 간 소비패턴을 분석했어요.", result.summaryText());
		assertEquals(3_000_000L, result.averageIncome());
		assertEquals(2_000_000L, result.averageExpense());
		assertEquals(1_000_000L, result.remainingAmount());
		assertEquals(60.0, result.fixedExpenseRatio());
		assertEquals(40.0, result.variableExpenseRatio());
		assertEquals("소비 절약이 필요해보여요.", result.insightMessage());
		assertEquals(4, result.categoryExpenses().size());
		assertEquals("주거비", result.categoryExpenses().get(0).categoryName());
		assertEquals(2_400_000L, result.categoryExpenses().get(0).amount());
		assertEquals("식비", result.categoryExpenses().get(1).categoryName());
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
			.transactionTypeName(transactionType == 1 ? "입금" : "출금")
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
			.billStatementsStatus("미결제")
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
			.analyzedAt(LocalDateTime.of(2026, 3, 31, 23, 59, 59))
			.build();
	}
}

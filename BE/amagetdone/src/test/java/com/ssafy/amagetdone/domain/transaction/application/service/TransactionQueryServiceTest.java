package com.ssafy.amagetdone.domain.transaction.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.lang.reflect.Method;

import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Card;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.entity.Merchant;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekCategoryRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.vo.UserStatus;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseGraphDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionListDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyExpenseGraphDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.application.usecase.SyncLatestTransactionsUseCase;
import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisMetadata;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisResult;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.repository.AccountTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.CardTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisMetadataRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisResultRepository;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionExcludedReason;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class TransactionQueryServiceTest {

	@Test
	void listTransactionsOverridesClassReadOnlyTransaction() throws Exception {
		Method method = TransactionQueryService.class.getDeclaredMethod(
			"listTransactions",
			Integer.class,
			TransactionListDto.Query.class
		);

		Transactional transactional = method.getAnnotation(Transactional.class);

		assertTrue(transactional != null, "listTransactions should declare its own @Transactional");
		assertFalse(transactional.readOnly(), "listTransactions must run in a writable transaction");
	}

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
		TransactionQueryService queryService = transactionQueryService();

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
		assertEquals(800_000L, result.categoryExpenses().get(0).amount());
		assertEquals("Food", result.categoryExpenses().get(1).categoryName());
		assertEquals(600_000L, result.categoryExpenses().get(1).amount());
	}

	@Test
	void listTransactionsUsesVisibleAccountMerchantText() {
		TransactionQueryService queryService = transactionQueryService();

		User user = User.builder()
			.id(1)
			.email("tester@example.com")
			.nickname("tester")
			.status(UserStatus.ACTIVE)
			.build();
		Account account = Account.create(user, "111-222-333", "type-1", "main", "deposit", "001", "bank", "KRW");
		Category defaultCategory = Category.create(4, "Other");
		AccountTransactionOverride accountTransaction = AccountTransactionOverride.builder()
			.id(1)
			.account(account)
			.category(defaultCategory)
			.transactionDateTime(LocalDateTime.of(2025, 12, 20, 12, 50, 18))
			.transactionBalance(90_000L)
			.transactionMemo("자금결제부")
			.transactionUniqueNo(1L)
			.transactionType(2)
			.transactionTypeName("OUT")
			.transactionAfterBalance(659_202L)
			.transactionSummary("타이짬뽕인동점")
			.build();

		given(syncLatestTransactionsUseCase.execute(1, 1, false))
			.willReturn(new com.ssafy.amagetdone.domain.transaction.application.service.TransactionSyncService.SyncResult(0, 0));
		given(accountTransactionOverrideRepository
			.findAllByAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(any(), any(), any()))
			.willReturn(List.of(accountTransaction));
		given(cardTransactionOverrideRepository
			.findAllByCard_WithdrawalAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(any(), any(), any()))
			.willReturn(List.of());
		given(transactionAnalysisResultRepository.findAllByTransaction_IdIn(anyList())).willReturn(List.of());

		TransactionListDto.Response response = queryService.listTransactions(
			1,
			new TransactionListDto.Query(
				java.time.LocalDate.of(2025, 12, 1),
				java.time.LocalDate.of(2025, 12, 31),
				"ALL",
				0,
				20
			)
		);

		assertEquals("타이짬뽕인동점", response.items().get(0).merchantText());
	}

	@Test
	void listTransactionsRequestsSyncForTheExplicitDateWindow() {
		TransactionQueryService queryService = transactionQueryService();
		LocalDate fromDate = LocalDate.of(2025, 12, 27);
		LocalDate toDate = LocalDate.of(2026, 3, 27);

		given(syncLatestTransactionsUseCase.execute(1, fromDate, toDate))
			.willReturn(new com.ssafy.amagetdone.domain.transaction.application.service.TransactionSyncService.SyncResult(0, 0));
		given(accountTransactionOverrideRepository
			.findAllByAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(any(), any(), any()))
			.willReturn(List.of());
		given(cardTransactionOverrideRepository
			.findAllByCard_WithdrawalAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(any(), any(), any()))
			.willReturn(List.of());

		TransactionListDto.Response response = queryService.listTransactions(
			1,
			new TransactionListDto.Query(fromDate, toDate, "ALL", 0, 20)
		);

		verify(syncLatestTransactionsUseCase).execute(1, fromDate, toDate);
		assertEquals(0L, response.totalElements());
	}

	@Test
	void getWeeklyExpenseGraphReturnsSevenDailyPointsIncludingZeroDays() {
		TransactionQueryService queryService = transactionQueryService();

		User user = User.builder()
			.id(1)
			.email("tester@example.com")
			.nickname("tester")
			.status(UserStatus.ACTIVE)
			.build();
		Account account = Account.create(user, "111-222-333", "type-1", "main", "deposit", "001", "bank", "KRW");
		Category food = Category.create(10, "Food");
		Category housing = Category.create(12, "Housing");
		AccountTransactionOverride mondayExpense = accountTransaction(1, account, food, LocalDateTime.of(2026, 3, 9, 12, 0), 50_000L, 2, "Lunch");
		AccountTransactionOverride wednesdayExpense = accountTransaction(2, account, housing, LocalDateTime.of(2026, 3, 11, 8, 0), 30_000L, 2, "Rent");
		AccountTransactionOverride sundayExpense = accountTransaction(3, account, food, LocalDateTime.of(2026, 3, 15, 20, 0), 20_000L, 2, "Dinner");
		AccountTransactionOverride income = accountTransaction(4, account, housing, LocalDateTime.of(2026, 3, 10, 9, 0), 300_000L, 1, "Salary");

		given(accountTransactionOverrideRepository
			.findAllByAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(any(), any(), any()))
			.willReturn(List.of(sundayExpense, wednesdayExpense, income, mondayExpense));
		given(cardTransactionOverrideRepository
			.findAllByCard_WithdrawalAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(any(), any(), any()))
			.willReturn(List.of());
		given(transactionAnalysisResultRepository.findAllByTransaction_IdIn(anyList()))
			.willReturn(List.of(
				includedResult(mondayExpense, food, ExpenseScope.VARIABLE),
				includedResult(wednesdayExpense, housing, ExpenseScope.FIXED),
				includedResult(sundayExpense, food, ExpenseScope.VARIABLE),
				excludedResult(income, housing, TransactionExcludedReason.INCOME)
			));

		WeeklyExpenseGraphDto.Response response = queryService.getWeeklyExpenseGraph(1, "2026-03-2");

		assertEquals("2026-03-2", response.yearWeek());
		assertEquals(LocalDate.of(2026, 3, 9), response.weekStartDate());
		assertEquals(LocalDate.of(2026, 3, 15), response.weekEndDate());
		assertEquals(100_000L, response.totalExpenseAmount());
		assertEquals(7, response.points().size());
		assertEquals(LocalDate.of(2026, 3, 9), response.points().get(0).date());
		assertEquals("월", response.points().get(0).label());
		assertEquals(50_000L, response.points().get(0).expenseAmount());
		assertEquals(LocalDate.of(2026, 3, 10), response.points().get(1).date());
		assertEquals(0L, response.points().get(1).expenseAmount());
		assertEquals(LocalDate.of(2026, 3, 11), response.points().get(2).date());
		assertEquals(30_000L, response.points().get(2).expenseAmount());
		assertEquals(LocalDate.of(2026, 3, 15), response.points().get(6).date());
		assertEquals(20_000L, response.points().get(6).expenseAmount());
		assertEquals(LocalDateTime.of(2026, 3, 1, 0, 0), response.lastAnalyzedAt());
	}

	@Test
	void getMonthlyExpenseGraphReturnsOverlappingWeeklyPointsUsingOnlyInMonthExpenses() {
		TransactionQueryService queryService = transactionQueryService();

		User user = User.builder()
			.id(1)
			.email("tester@example.com")
			.nickname("tester")
			.status(UserStatus.ACTIVE)
			.build();
		Account account = Account.create(user, "111-222-333", "type-1", "main", "deposit", "001", "bank", "KRW");
		Category food = Category.create(10, "Food");
		AccountTransactionOverride febExpense = accountTransaction(1, account, food, LocalDateTime.of(2026, 2, 28, 12, 0), 30_000L, 2, "February");
		AccountTransactionOverride marchFirstExpense = accountTransaction(2, account, food, LocalDateTime.of(2026, 3, 1, 18, 0), 10_000L, 2, "March 1");
		AccountTransactionOverride marchSecondWeekExpense = accountTransaction(3, account, food, LocalDateTime.of(2026, 3, 3, 12, 0), 20_000L, 2, "March 3");
		AccountTransactionOverride marchFourthWeekExpense = accountTransaction(4, account, food, LocalDateTime.of(2026, 3, 17, 12, 0), 40_000L, 2, "March 17");
		AccountTransactionOverride marchLastWeekExpense = accountTransaction(5, account, food, LocalDateTime.of(2026, 3, 31, 12, 0), 50_000L, 2, "March 31");
		AccountTransactionOverride aprilExpense = accountTransaction(6, account, food, LocalDateTime.of(2026, 4, 2, 12, 0), 60_000L, 2, "April");

		given(accountTransactionOverrideRepository
			.findAllByAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(any(), any(), any()))
			.willReturn(List.of(aprilExpense, marchLastWeekExpense, marchFourthWeekExpense, marchSecondWeekExpense, marchFirstExpense, febExpense));
		given(cardTransactionOverrideRepository
			.findAllByCard_WithdrawalAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(any(), any(), any()))
			.willReturn(List.of());
		given(transactionAnalysisResultRepository.findAllByTransaction_IdIn(anyList()))
			.willReturn(List.of(
				includedResult(febExpense, food, ExpenseScope.VARIABLE),
				includedResult(marchFirstExpense, food, ExpenseScope.VARIABLE),
				includedResult(marchSecondWeekExpense, food, ExpenseScope.VARIABLE),
				includedResult(marchFourthWeekExpense, food, ExpenseScope.VARIABLE),
				includedResult(marchLastWeekExpense, food, ExpenseScope.VARIABLE),
				includedResult(aprilExpense, food, ExpenseScope.VARIABLE)
			));

		MonthlyExpenseGraphDto.Response response = queryService.getMonthlyExpenseGraph(1, "2026-03");

		assertEquals("2026-03", response.yearMonth());
		assertEquals(LocalDate.of(2026, 3, 1), response.monthStartDate());
		assertEquals(LocalDate.of(2026, 3, 31), response.monthEndDate());
		assertEquals(120_000L, response.totalExpenseAmount());
		assertEquals(6, response.points().size());
		assertEquals("2026-02-4", response.points().get(0).yearWeek());
		assertEquals(LocalDate.of(2026, 2, 23), response.points().get(0).weekStartDate());
		assertEquals(LocalDate.of(2026, 3, 1), response.points().get(0).weekEndDate());
		assertEquals("3월 1주차", response.points().get(0).label());
		assertEquals(10_000L, response.points().get(0).expenseAmount());
		assertEquals("3월 2주차", response.points().get(1).label());
		assertEquals(20_000L, response.points().get(1).expenseAmount());
		assertEquals(0L, response.points().get(2).expenseAmount());
		assertEquals("3월 4주차", response.points().get(3).label());
		assertEquals(40_000L, response.points().get(3).expenseAmount());
		assertEquals(0L, response.points().get(4).expenseAmount());
		assertEquals("2026-03-5", response.points().get(5).yearWeek());
		assertEquals(50_000L, response.points().get(5).expenseAmount());
		assertEquals(LocalDateTime.of(2026, 3, 1, 0, 0), response.lastAnalyzedAt());
	}

	@Test
	void getWeeklyTransactionAnalysisAggregatesActualEssentialExpensesWithoutBudgetData() {
		TransactionQueryService queryService = transactionQueryService();

		User user = User.builder()
			.id(1)
			.email("tester@example.com")
			.nickname("tester")
			.status(UserStatus.ACTIVE)
			.build();
		TransactionAnalysisMetadata latestMetadata = TransactionAnalysisMetadata.builder()
			.id(1)
			.user(user)
			.requestedMonths(3)
			.windowStartAt(LocalDateTime.of(2025, 12, 1, 0, 0))
			.windowEndAt(LocalDateTime.of(2026, 2, 28, 23, 59, 59))
			.transactionCount(9)
			.latestTransactionAt(LocalDateTime.of(2026, 2, 20, 12, 0))
			.analyzedAt(LocalDateTime.of(2026, 3, 1, 0, 0))
			.build();

		given(transactionAnalysisMetadataRepository.findTopByUser_IdOrderByAnalyzedAtDesc(1))
			.willReturn(Optional.of(latestMetadata));
		Category food = Category.create(10, "Food");
		Category transport = Category.create(11, "Transport");
		Category housing = Category.create(12, "Housing");
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
		AccountTransactionOverride salary = accountTransaction(
			1,
			account,
			housing,
			LocalDateTime.of(2026, 3, 17, 9, 0),
			300_000L,
			1,
			"Salary"
		);
		AccountTransactionOverride rent = accountTransaction(
			2,
			account,
			housing,
			LocalDateTime.of(2026, 3, 18, 8, 0),
			70_000L,
			2,
			"Rent"
		);
		CardTransactionOverride lunch = cardTransaction(
			3,
			card,
			merchant,
			food,
			LocalDateTime.of(2026, 3, 19, 12, 0),
			45_000L,
			"Lunch"
		);
		CardTransactionOverride bus = cardTransaction(
			4,
			card,
			merchant,
			transport,
			LocalDateTime.of(2026, 3, 20, 18, 30),
			20_000L,
			"Bus"
		);
		CardTransactionOverride movie = cardTransaction(
			5,
			card,
			merchant,
			transport,
			LocalDateTime.of(2026, 3, 21, 20, 0),
			30_000L,
			"Movie"
		);

		given(accountTransactionOverrideRepository
			.findAllByAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(any(), any(), any()))
			.willReturn(List.of(salary, rent));
		given(cardTransactionOverrideRepository
			.findAllByCard_WithdrawalAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(any(), any(), any()))
			.willReturn(List.of(lunch, bus, movie));
		given(transactionAnalysisResultRepository.findAllByTransaction_IdInAndAnalyzedAt(anyList(), any()))
			.willReturn(List.of(
				excludedResult(salary, housing, TransactionExcludedReason.INCOME),
				includedResult(rent, housing, ExpenseScope.FIXED),
				includedResult(lunch, food, ExpenseScope.VARIABLE),
				includedResult(bus, transport, ExpenseScope.VARIABLE),
				excludedResult(movie, transport, TransactionExcludedReason.NON_ESSENTIAL)
			));

		WeeklyTransactionAnalysisDto.Response response = queryService.getWeeklyTransactionAnalysis(1, "2026-03-3");

		assertEquals("2026-03-3", response.yearWeek());
		assertEquals(LocalDate.of(2026, 3, 16), response.weekStartDate());
		assertEquals(LocalDate.of(2026, 3, 22), response.weekEndDate());
		assertEquals(165_000L, response.expenseAmount());
		assertEquals(135_000L, response.essentialAmount());
		assertEquals(70_000L, response.fixedEssentialAmount());
		assertEquals(65_000L, response.variableEssentialAmount());
		assertEquals(3, response.categoryExpenses().size());
		assertEquals("Food", response.categoryExpenses().get(0).categoryName());
		assertEquals(0L, response.categoryExpenses().get(0).plannedAmount());
		assertEquals(45_000L, response.categoryExpenses().get(0).actualSpentAmount());
		assertEquals(0L, response.categoryExpenses().get(0).remainingAmount());
		assertEquals("Transport", response.categoryExpenses().get(1).categoryName());
		assertEquals(0L, response.categoryExpenses().get(1).plannedAmount());
		assertEquals(50_000L, response.categoryExpenses().get(1).actualSpentAmount());
		assertEquals(0L, response.categoryExpenses().get(1).remainingAmount());
		assertEquals("Housing", response.categoryExpenses().get(2).categoryName());
		assertEquals(0L, response.categoryExpenses().get(2).plannedAmount());
		assertEquals(70_000L, response.categoryExpenses().get(2).actualSpentAmount());
		assertEquals(0L, response.categoryExpenses().get(2).remainingAmount());
		assertEquals(
			response.expenseAmount(),
			response.categoryExpenses().stream().mapToLong(WeeklyTransactionAnalysisDto.CategoryExpense::actualSpentAmount).sum()
		);
		assertEquals(latestMetadata.getAnalyzedAt(), response.lastAnalyzedAt());
	}

	@Test
	void getWeeklyTransactionAnalysisReturnsEmptyResponseWhenWeekHasNoExpenses() {
		TransactionQueryService queryService = transactionQueryService();

		TransactionAnalysisMetadata latestMetadata = TransactionAnalysisMetadata.builder()
			.id(1)
			.user(User.builder()
				.id(1)
				.email("tester@example.com")
				.nickname("tester")
				.status(UserStatus.ACTIVE)
				.build())
			.analyzedAt(LocalDateTime.of(2026, 3, 1, 0, 0))
			.build();
		given(transactionAnalysisMetadataRepository.findTopByUser_IdOrderByAnalyzedAtDesc(1))
			.willReturn(Optional.of(latestMetadata));
		given(accountTransactionOverrideRepository
			.findAllByAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(any(), any(), any()))
			.willReturn(List.of());
		given(cardTransactionOverrideRepository
			.findAllByCard_WithdrawalAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(any(), any(), any()))
			.willReturn(List.of());

		WeeklyTransactionAnalysisDto.Response response = queryService.getWeeklyTransactionAnalysis(1, "2026-03-3");

		assertEquals("2026-03-3", response.yearWeek());
		assertEquals(0L, response.expenseAmount());
		assertEquals(0L, response.essentialAmount());
		assertEquals(0L, response.fixedEssentialAmount());
		assertEquals(0L, response.variableEssentialAmount());
		assertTrue(response.categoryExpenses().isEmpty());
		assertNull(response.weekStartDate());
		assertNull(response.weekEndDate());
		assertNull(response.lastAnalyzedAt());
	}

	@Test
	void getMonthlyTransactionAnalysisAggregatesCategoryExpensesFromAllPureExpenses() {
		TransactionQueryService queryService = transactionQueryService();

		User user = User.builder()
			.id(1)
			.email("tester@example.com")
			.nickname("tester")
			.status(UserStatus.ACTIVE)
			.build();
		TransactionAnalysisMetadata latestMetadata = TransactionAnalysisMetadata.builder()
			.id(1)
			.user(user)
			.requestedMonths(3)
			.windowStartAt(LocalDateTime.of(2025, 12, 1, 0, 0))
			.windowEndAt(LocalDateTime.of(2026, 2, 28, 23, 59, 59))
			.transactionCount(12)
			.latestTransactionAt(LocalDateTime.of(2026, 3, 28, 18, 0))
			.analyzedAt(LocalDateTime.of(2026, 4, 1, 0, 0))
			.build();
		given(transactionAnalysisMetadataRepository.findTopByUser_IdOrderByAnalyzedAtDesc(1))
			.willReturn(Optional.of(latestMetadata));

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
		Category food = Category.create(10, "Food");
		Category housing = Category.create(12, "Housing");
		Category leisure = Category.create(40, "Leisure");

		AccountTransactionOverride rent = accountTransaction(1, account, housing, LocalDateTime.of(2026, 3, 5, 9, 0), 50_000L, 2, "Rent");
		CardTransactionOverride groceries = cardTransaction(2, card, merchant, food, LocalDateTime.of(2026, 3, 10, 12, 0), 80_000L, "Groceries");
		CardTransactionOverride movie = cardTransaction(3, card, merchant, leisure, LocalDateTime.of(2026, 3, 20, 20, 0), 40_000L, "Movie");

		given(accountTransactionOverrideRepository
			.findAllByAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(any(), any(), any()))
			.willReturn(List.of(rent));
		given(cardTransactionOverrideRepository
			.findAllByCard_WithdrawalAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(any(), any(), any()))
			.willReturn(List.of(groceries, movie));
		given(transactionAnalysisResultRepository.findAllByTransaction_IdInAndAnalyzedAt(anyList(), any()))
			.willReturn(List.of(
				includedResult(rent, housing, ExpenseScope.FIXED),
				includedResult(groceries, food, ExpenseScope.VARIABLE),
				excludedResult(movie, leisure, TransactionExcludedReason.NON_ESSENTIAL)
			));

		EssentialExpenseSnapshotWeek snapshotWeek = EssentialExpenseSnapshotWeek.builder()
			.id(100)
			.user(user)
			.yearWeek("2026-W10")
			.weekStartDate(LocalDate.of(2026, 3, 2))
			.weekEndDate(LocalDate.of(2026, 3, 8))
			.baseMonths(3)
			.essentialAmount(130_000L)
			.fixedEssentialAmount(50_000L)
			.variableEssentialAmount(80_000L)
			.analyzedAt(latestMetadata.getAnalyzedAt())
			.build();
		given(essentialExpenseSnapshotWeekRepository
			.findAllByUser_IdAndAnalyzedAtAndWeekStartDateBetweenOrderByWeekStartDate(any(), any(), any(), any()))
			.willReturn(List.of(snapshotWeek));

		MonthlyTransactionAnalysisDto.Response response = queryService.getMonthlyTransactionAnalysis(1, "2026-03");

		assertEquals("2026-03", response.yearMonth());
		assertEquals(170_000L, response.expenseAmount());
		assertEquals(130_000L, response.monthlyEssentialAmount());
		assertEquals(50_000L, response.fixedEssentialAmount());
		assertEquals(80_000L, response.variableEssentialAmount());
		assertEquals(3, response.categoryExpenses().size());
		Map<String, MonthlyTransactionAnalysisDto.CategoryExpense> categoryByName = response.categoryExpenses().stream()
			.collect(java.util.stream.Collectors.toMap(MonthlyTransactionAnalysisDto.CategoryExpense::categoryName, item -> item));
		assertEquals(80_000L, categoryByName.get("Food").amount());
		assertEquals(47.06, categoryByName.get("Food").ratio());
		assertEquals(50_000L, categoryByName.get("Housing").amount());
		assertEquals(29.41, categoryByName.get("Housing").ratio());
		assertEquals(40_000L, categoryByName.get("Leisure").amount());
		assertEquals(23.53, categoryByName.get("Leisure").ratio());
		assertEquals(
			response.expenseAmount(),
			response.categoryExpenses().stream().mapToLong(MonthlyTransactionAnalysisDto.CategoryExpense::amount).sum()
		);
	}

	@Test
	void getWeeklyTransactionAnalysisRejectsIsoYearWeekInput() {
		TransactionQueryService queryService = transactionQueryService();

		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> queryService.getWeeklyTransactionAnalysis(1, "2026-W12")
		);

		assertEquals("yearWeek must follow yyyy-MM-w.", exception.getMessage());
	}

	private TransactionQueryService transactionQueryService() {
		return new TransactionQueryService(
			accountTransactionOverrideRepository,
			cardTransactionOverrideRepository,
			transactionAnalysisResultRepository,
			transactionAnalysisMetadataRepository,
			essentialExpenseSnapshotWeekRepository,
			essentialExpenseSnapshotWeekCategoryRepository,
			syncLatestTransactionsUseCase
		);
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

	private TransactionAnalysisResult excludedResult(
		TransactionOverride transaction,
		Category category,
		TransactionExcludedReason excludedReason
	) {
		return TransactionAnalysisResult.builder()
			.id(transaction.getId())
			.transaction(transaction)
			.category(category)
			.analysisIncluded(false)
			.excludedReason(excludedReason)
			.analyzedAt(LocalDateTime.of(2026, 3, 1, 0, 0))
			.build();
	}
}

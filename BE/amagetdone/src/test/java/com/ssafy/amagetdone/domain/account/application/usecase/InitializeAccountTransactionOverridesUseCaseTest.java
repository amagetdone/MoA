package com.ssafy.amagetdone.domain.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.account.support.util.AccountRealTransactionScenario;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionCategoryAssignmentService;
import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.repository.AccountTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisResultRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InitializeAccountTransactionOverridesUseCaseTest {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private AccountTransactionOverrideRepository accountTransactionOverrideRepository;

	@Mock
	private TransactionAnalysisResultRepository transactionAnalysisResultRepository;

	@Mock
	private TransactionCategoryAssignmentService transactionCategoryAssignmentService;

	@Test
	void executeDeletesLinkedAnalysisResultsBeforeReloadingAccountTransactions() {
		Account account = Account.create(
			User.builder().id(1).build(),
			"0012345678901234",
			"product-1",
			"Main account",
			"Demand deposit",
			"001",
			"Bank",
			"KRW"
		);
		Category category = Category.create(4, "etc");
		InitializeAccountTransactionOverridesUseCase useCase = new InitializeAccountTransactionOverridesUseCase(
			accountRepository,
			accountTransactionOverrideRepository,
			transactionAnalysisResultRepository,
			new AccountRealTransactionScenario(),
			transactionCategoryAssignmentService
		);
		AccountTransactionOverride existing = AccountTransactionOverride.builder().id(101).account(account).build();
		given(accountRepository.findByAccountNo("0012345678901234")).willReturn(Optional.of(account));
		given(accountTransactionOverrideRepository.findAllByAccount_Id(account.getId())).willReturn(List.of(existing));
		given(transactionCategoryAssignmentService.assignCategories(org.mockito.ArgumentMatchers.anyList())).willAnswer(invocation -> {
			List<String> texts = invocation.getArgument(0);
			return texts.stream().collect(Collectors.toMap(Function.identity(), value -> category, (left, right) -> left));
		});

		AccountAdminDto.TransactionInitResponse response =
			useCase.execute(new AccountAdminDto.AccountTransactionInitRequest("0012345678901234"));

		ArgumentCaptor<List<AccountTransactionOverride>> captor = listCaptor();
		verify(transactionAnalysisResultRepository).deleteByTransaction_IdIn(List.of(101));
		verify(accountTransactionOverrideRepository).deleteByAccount_Id(account.getId());
		verify(transactionCategoryAssignmentService).assignCategories(org.mockito.ArgumentMatchers.anyList());
		verify(accountTransactionOverrideRepository).saveAll(captor.capture());
		assertThat(response.targetType()).isEqualTo("account");
		assertThat(response.targetNo()).isEqualTo("0012345678901234");
		assertThat(response.transactionCount()).isEqualTo(captor.getValue().size());
		assertThat(captor.getValue().size()).isBetween(40, 80);
		assertThat(captor.getValue())
			.extracting(AccountTransactionOverride::getTransactionDateTime)
			.map(YearMonth::from)
			.contains(YearMonth.of(2025, 12), YearMonth.of(2026, 1), YearMonth.of(2026, 2), YearMonth.of(2026, 3));
		assertThat(captor.getValue())
			.extracting(AccountTransactionOverride::getTransactionDateTime)
			.map(dateTime -> dateTime.toLocalDate())
			.contains(LocalDate.of(2026, 3, 20));
		assertThat(countDirectSpendingLikeTransactions(captor.getValue())).isGreaterThanOrEqualTo(20);
		assertThat(countTransferLikeTransactions(captor.getValue())).isLessThanOrEqualTo(8);
	}

	@Test
	void executeThrowsWhenAccountDoesNotExist() {
		InitializeAccountTransactionOverridesUseCase useCase = new InitializeAccountTransactionOverridesUseCase(
			accountRepository,
			accountTransactionOverrideRepository,
			transactionAnalysisResultRepository,
			new AccountRealTransactionScenario(),
			transactionCategoryAssignmentService
		);
		given(accountRepository.findByAccountNo("missing")).willReturn(Optional.empty());

		assertThatThrownBy(() -> useCase.execute(new AccountAdminDto.AccountTransactionInitRequest("missing")))
			.isInstanceOf(AccountException.class)
			.extracting(exception -> ((AccountException)exception).getErrorCode())
			.isEqualTo(AccountErrorCode.ACCOUNT_NOT_FOUND);
	}

	@Test
	void executeUsesTransactionSummaryBeforeMemoForCategoryAssignment() {
		Account account = Account.create(
			User.builder().id(1).build(),
			"0012345678901234",
			"product-1",
			"Main account",
			"Demand deposit",
			"001",
			"Bank",
			"KRW"
		);
		Category category = Category.create(4, "etc");
		InitializeAccountTransactionOverridesUseCase useCase = new InitializeAccountTransactionOverridesUseCase(
			accountRepository,
			accountTransactionOverrideRepository,
			transactionAnalysisResultRepository,
			new AccountRealTransactionScenario(),
			transactionCategoryAssignmentService
		);
		given(accountRepository.findByAccountNo("0012345678901234")).willReturn(Optional.of(account));
		given(accountTransactionOverrideRepository.findAllByAccount_Id(account.getId())).willReturn(List.of());
		given(transactionCategoryAssignmentService.assignCategories(org.mockito.ArgumentMatchers.anyList())).willAnswer(invocation -> {
			List<String> texts = invocation.getArgument(0);
			return texts.stream().collect(Collectors.toMap(Function.identity(), value -> category, (left, right) -> left));
		});

		useCase.execute(new AccountAdminDto.AccountTransactionInitRequest("0012345678901234"));

		ArgumentCaptor<List<String>> captor = listCaptor();
		verify(transactionCategoryAssignmentService).assignCategories(captor.capture());
		assertThat(captor.getValue()).contains("버거킹구미인동점");
		assertThat(captor.getValue()).doesNotContain("생활비계좌");
	}

	@SuppressWarnings("unchecked")
	private <T> ArgumentCaptor<List<T>> listCaptor() {
		return (ArgumentCaptor<List<T>>) (ArgumentCaptor<?>)ArgumentCaptor.forClass(List.class);
	}

	private long countDirectSpendingLikeTransactions(List<AccountTransactionOverride> transactions) {
		return transactions.stream()
			.map(transaction -> transaction.getTransactionSummary() + " " + transaction.getTransactionMemo())
			.filter(text -> text.contains("버거")
				|| text.contains("김밥")
				|| text.contains("마라")
				|| text.contains("국밥")
				|| text.contains("카페")
				|| text.contains("택시")
				|| text.contains("버스")
				|| text.contains("지하철"))
			.count();
	}

	private long countTransferLikeTransactions(List<AccountTransactionOverride> transactions) {
		return transactions.stream()
			.map(transaction -> transaction.getTransactionSummary() + " " + transaction.getTransactionMemo())
			.filter(text -> text.contains("펌뱅킹")
				|| text.contains("오픈뱅킹")
				|| text.contains("자금결제부")
				|| text.contains("카드결제")
				|| text.matches(".*김\\d\\d.*"))
			.count();
	}
}

package com.ssafy.amagetdone.domain.account.application.usecase;

import java.util.List;
import java.util.Map;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.account.support.util.AccountRealTransactionScenario;
import com.ssafy.amagetdone.domain.account.support.util.AccountRealTransactionScenario.AccountSeed;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionCategoryAssignmentService;
import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.repository.AccountTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisResultRepository;
import com.ssafy.amagetdone.domain.transaction.support.util.TransactionMerchantTextResolver;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InitializeAccountTransactionOverridesUseCase {

	private static final String DEPOSIT_TYPE_NAME = "\uC785\uAE08";
	private static final String WITHDRAWAL_TYPE_NAME = "\uCD9C\uAE08";

	private final AccountRepository accountRepository;
	private final AccountTransactionOverrideRepository accountTransactionOverrideRepository;
	private final TransactionAnalysisResultRepository transactionAnalysisResultRepository;
	private final AccountRealTransactionScenario accountRealTransactionScenario;
	private final TransactionCategoryAssignmentService transactionCategoryAssignmentService;

	public InitializeAccountTransactionOverridesUseCase(
		AccountRepository accountRepository,
		AccountTransactionOverrideRepository accountTransactionOverrideRepository,
		TransactionAnalysisResultRepository transactionAnalysisResultRepository,
		AccountRealTransactionScenario accountRealTransactionScenario,
		TransactionCategoryAssignmentService transactionCategoryAssignmentService
	) {
		this.accountRepository = accountRepository;
		this.accountTransactionOverrideRepository = accountTransactionOverrideRepository;
		this.transactionAnalysisResultRepository = transactionAnalysisResultRepository;
		this.accountRealTransactionScenario = accountRealTransactionScenario;
		this.transactionCategoryAssignmentService = transactionCategoryAssignmentService;
	}

	@Transactional
	public AccountAdminDto.TransactionInitResponse execute(AccountAdminDto.AccountTransactionInitRequest request) {
		validateRequest(request == null ? null : request.accountNo());
		Account account = accountRepository.findByAccountNo(request.accountNo())
			.orElseThrow(() -> new AccountException(AccountErrorCode.ACCOUNT_NOT_FOUND));
		List<AccountSeed> seeds = accountRealTransactionScenario.completedMonthSeeds();
		if (seeds.isEmpty()) {
			throw new AccountException(AccountErrorCode.TRANSACTION_INIT_SEED_NOT_FOUND);
		}
		Map<String, Category> assignedCategories = transactionCategoryAssignmentService.assignCategories(
			seeds.stream()
				.map(this::extractCategoryText)
				.toList()
		);

		deleteExistingAnalysisResults(account.getId());
		accountTransactionOverrideRepository.deleteByAccount_Id(account.getId());
		accountTransactionOverrideRepository.saveAll(
			seeds.stream()
				.map(seed -> AccountTransactionOverride.builder()
					.category(resolveCategory(assignedCategories, extractCategoryText(seed)))
					.transactionDateTime(seed.transactionDateTime())
					.transactionBalance(seed.transactionBalance())
					.transactionMemo(seed.transactionMemo())
					.transactionUniqueNo(seed.transactionUniqueNo())
					.transactionType(seed.transactionType())
					.transactionTypeName(seed.transactionType() == 1 ? DEPOSIT_TYPE_NAME : WITHDRAWAL_TYPE_NAME)
					.transactionAfterBalance(seed.transactionAfterBalance())
					.transactionSummary(seed.transactionSummary())
					.account(account)
					.build())
				.toList()
		);

		return new AccountAdminDto.TransactionInitResponse(
			"account",
			account.getAccountNo(),
			seeds.size()
		);
	}

	private void validateRequest(String accountNo) {
		if (accountNo == null || accountNo.isBlank()) {
			throw new AccountException(AccountErrorCode.INVALID_DATA_INIT_REQUEST);
		}
	}

	private void deleteExistingAnalysisResults(String accountId) {
		List<Integer> existingTransactionIds = accountTransactionOverrideRepository.findAllByAccount_Id(accountId).stream()
			.map(AccountTransactionOverride::getId)
			.toList();
		if (!existingTransactionIds.isEmpty()) {
			transactionAnalysisResultRepository.deleteByTransaction_IdIn(existingTransactionIds);
		}
	}

	private String extractCategoryText(AccountSeed seed) {
		return TransactionMerchantTextResolver.resolveAccountText(seed.transactionSummary(), seed.transactionMemo());
	}

	private Category resolveCategory(Map<String, Category> assignedCategories, String sourceText) {
		Category category = assignedCategories.get(sourceText);
		if (category == null) {
			throw new AccountException(AccountErrorCode.ACCOUNT_ERROR);
		}
		return category;
	}
}

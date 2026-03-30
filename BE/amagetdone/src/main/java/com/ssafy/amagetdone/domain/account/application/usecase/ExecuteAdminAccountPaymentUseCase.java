package com.ssafy.amagetdone.domain.account.application.usecase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.infra.client.AccountSsafyApiProperties;
import com.ssafy.amagetdone.domain.account.infra.client.SsafyAdminDemandDepositApiClient;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionCategoryAssignmentService;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.DemandDepositTransactionDto;
import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.repository.AccountTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.support.util.TransactionMerchantTextResolver;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExecuteAdminAccountPaymentUseCase {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

	private final AccountRepository accountRepository;
	private final AccountTransactionOverrideRepository accountTransactionOverrideRepository;
	private final SsafyAdminDemandDepositApiClient ssafyAdminDemandDepositApiClient;
	private final TransactionCategoryAssignmentService transactionCategoryAssignmentService;
	private final AccountSsafyApiProperties properties;

	public ExecuteAdminAccountPaymentUseCase(
		AccountRepository accountRepository,
		AccountTransactionOverrideRepository accountTransactionOverrideRepository,
		SsafyAdminDemandDepositApiClient ssafyAdminDemandDepositApiClient,
		TransactionCategoryAssignmentService transactionCategoryAssignmentService,
		AccountSsafyApiProperties properties
	) {
		this.accountRepository = accountRepository;
		this.accountTransactionOverrideRepository = accountTransactionOverrideRepository;
		this.ssafyAdminDemandDepositApiClient = ssafyAdminDemandDepositApiClient;
		this.transactionCategoryAssignmentService = transactionCategoryAssignmentService;
		this.properties = properties;
	}

	@Transactional
	public AccountAdminDto.PaymentResponse execute(Integer userId, AccountAdminDto.AccountPaymentRequest request) {
		Account account = accountRepository.findActiveLinkedAccountByUserIdAndAccountNo(userId, request.accountNo())
			.orElseThrow(() -> new AccountException(AccountErrorCode.ACCOUNT_NOT_FOUND));

		DemandDepositTransactionDto.Item item = ssafyAdminDemandDepositApiClient.createTransferTransaction(
			properties.getApiKey(),
			account.getUser().getUserKey(),
			request.accountNo(),
			request.counterpartyAccountNo(),
			request.amount(),
			request.withdrawalSummary(),
			request.depositSummary()
		);

		AccountTransactionOverride override = AccountTransactionOverride.builder()
			.category(resolveCategory(item))
			.transactionDateTime(toDateTime(item.transactionDate(), item.transactionTime()))
			.transactionBalance(item.transactionBalance())
			.transactionMemo(resolveMemo(item))
			.transactionUniqueNo(item.transactionUniqueNo())
			.transactionType(parseInteger(item.transactionType()))
			.transactionTypeName(item.transactionTypeName())
			.transactionAfterBalance(item.transactionAfterBalance())
			.transactionSummary(item.transactionSummary())
			.account(account)
			.build();
		accountTransactionOverrideRepository.save(override);

		return new AccountAdminDto.PaymentResponse(
			"account",
			account.getAccountNo(),
			item.transactionUniqueNo(),
			item.transactionDate(),
			item.transactionTime(),
			item.transactionBalance(),
			"COMPLETED"
		);
	}

	private Category resolveCategory(DemandDepositTransactionDto.Item item) {
		String sourceText = TransactionMerchantTextResolver.resolve(item);
		Map<String, Category> categories = transactionCategoryAssignmentService.assignCategories(java.util.List.of(sourceText));
		Category category = categories.get(sourceText);
		if (category == null) {
			throw new AccountException(AccountErrorCode.ACCOUNT_ERROR);
		}
		return category;
	}

	private String resolveMemo(DemandDepositTransactionDto.Item item) {
		if (item.transactionMemo() != null && !item.transactionMemo().isBlank()) {
			return item.transactionMemo();
		}
		return item.transactionSummary();
	}

	private LocalDateTime toDateTime(String transactionDate, String transactionTime) {
		return LocalDateTime.of(
			LocalDate.parse(transactionDate, DATE_FORMATTER),
			LocalTime.parse(normalizeTime(transactionTime), TIME_FORMATTER)
		);
	}

	private String normalizeTime(String transactionTime) {
		if (transactionTime == null || transactionTime.isBlank()) {
			return "000000";
		}
		return transactionTime.length() == 6 ? transactionTime : String.format("%1$" + 6 + "s", transactionTime).replace(' ', '0');
	}

	private Integer parseInteger(String value) {
		if (value == null || value.isBlank()) {
			return 0;
		}
		return Integer.parseInt(value);
	}

}

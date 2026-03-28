package com.ssafy.amagetdone.domain.account.application.usecase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import com.ssafy.amagetdone.domain.account.api.dto.AccountDto;
import com.ssafy.amagetdone.domain.account.infra.client.SsafyDemandDepositAccountApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositBalanceDto;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositTransferDto;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.repository.CategoryRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.repository.AccountTransactionOverrideRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DepositAccountUseCase {

	private static final int DEFAULT_CATEGORY_ID = 4;
	private static final int DEPOSIT_TRANSACTION_TYPE = 1;
	private static final String DEPOSIT_TRANSACTION_TYPE_NAME = "입금";
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

	private final FindAccountUserUseCase findAccountUserUseCase;
	private final FindUserAccountUseCase findUserAccountUseCase;
	private final SsafyDemandDepositAccountApiClient accountApiClient;
	private final AccountTransactionOverrideRepository accountTransactionOverrideRepository;
	private final CategoryRepository categoryRepository;

	public DepositAccountUseCase(
		FindAccountUserUseCase findAccountUserUseCase,
		FindUserAccountUseCase findUserAccountUseCase,
		SsafyDemandDepositAccountApiClient accountApiClient,
		AccountTransactionOverrideRepository accountTransactionOverrideRepository,
		CategoryRepository categoryRepository
	) {
		this.findAccountUserUseCase = findAccountUserUseCase;
		this.findUserAccountUseCase = findUserAccountUseCase;
		this.accountApiClient = accountApiClient;
		this.accountTransactionOverrideRepository = accountTransactionOverrideRepository;
		this.categoryRepository = categoryRepository;
	}

	@Transactional
	public DemandDepositTransferDto.DepositResponse execute(Integer userId, AccountDto.DepositRequest request) {
		User user = findAccountUserUseCase.execute(userId);
		Account account = findUserAccountUseCase.execute(userId, request.accountNo());

		DemandDepositTransferDto.DepositResponse depositResponse = accountApiClient.deposit(
			new DemandDepositTransferDto.DepositRequest(
				user.getUserKey(),
				request.accountNo(),
				request.transactionBalance(),
				request.transactionSummary()
			)
		);

		DemandDepositBalanceDto.Response balanceResponse = accountApiClient.inquireBalance(
			new DemandDepositBalanceDto.Request(user.getUserKey(), request.accountNo())
		);

		String memo = request.transactionSummary() != null && !request.transactionSummary().isBlank()
			? request.transactionSummary()
			: DEPOSIT_TRANSACTION_TYPE_NAME;
		Category defaultCategory = categoryRepository.getReferenceById(DEFAULT_CATEGORY_ID);

		AccountTransactionOverride override = AccountTransactionOverride.builder()
			.category(defaultCategory)
			.transactionDateTime(toDateTime(depositResponse.transactionDate()))
			.transactionBalance(request.transactionBalance())
			.transactionMemo(memo)
			.transactionUniqueNo(depositResponse.transactionUniqueNo())
			.transactionType(DEPOSIT_TRANSACTION_TYPE)
			.transactionTypeName(DEPOSIT_TRANSACTION_TYPE_NAME)
			.transactionAfterBalance(balanceResponse.accountBalance())
			.transactionSummary(memo)
			.account(account)
			.build();
		accountTransactionOverrideRepository.save(override);

		return depositResponse;
	}

	private LocalDateTime toDateTime(String transactionDate) {
		return LocalDateTime.of(
			LocalDate.parse(transactionDate, DATE_FORMATTER),
			LocalTime.now()
		);
	}
}

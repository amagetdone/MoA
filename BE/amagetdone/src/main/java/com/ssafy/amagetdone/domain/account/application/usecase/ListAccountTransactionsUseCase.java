package com.ssafy.amagetdone.domain.account.application.usecase;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.transaction.infra.client.SsafyDemandDepositTransactionApiClient;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.DemandDepositTransactionDto;

import org.springframework.stereotype.Component;

@Component
public class ListAccountTransactionsUseCase {

	private final FindAccountUserUseCase findAccountUserUseCase;
	private final FindUserAccountUseCase findUserAccountUseCase;
	private final SsafyDemandDepositTransactionApiClient transactionApiClient;

	public ListAccountTransactionsUseCase(
		FindAccountUserUseCase findAccountUserUseCase,
		FindUserAccountUseCase findUserAccountUseCase,
		SsafyDemandDepositTransactionApiClient transactionApiClient
	) {
		this.findAccountUserUseCase = findAccountUserUseCase;
		this.findUserAccountUseCase = findUserAccountUseCase;
		this.transactionApiClient = transactionApiClient;
	}

	public DemandDepositTransactionDto.Response execute(
		Integer userId,
		String accountNo,
		String startDate,
		String endDate,
		String transactionType,
		String orderByType
	) {
		User user = findAccountUserUseCase.execute(userId);
		findUserAccountUseCase.execute(userId, accountNo);
		return transactionApiClient.inquireTransactions(
			new DemandDepositTransactionDto.Request(
				user.getUserKey(),
				accountNo,
				startDate,
				endDate,
				hasText(transactionType) ? transactionType : "A",
				hasText(orderByType) ? orderByType : "ASC"
			)
		);
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}

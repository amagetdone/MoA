package com.ssafy.amagetdone.domain.account.application.usecase;

import com.ssafy.amagetdone.domain.account.api.dto.AccountDto;
import com.ssafy.amagetdone.domain.account.infra.client.SsafyDemandDepositAccountApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositTransferDto;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.springframework.stereotype.Component;

@Component
public class DepositAccountUseCase {

	private final FindAccountUserUseCase findAccountUserUseCase;
	private final FindUserAccountUseCase findUserAccountUseCase;
	private final SsafyDemandDepositAccountApiClient accountApiClient;

	public DepositAccountUseCase(
		FindAccountUserUseCase findAccountUserUseCase,
		FindUserAccountUseCase findUserAccountUseCase,
		SsafyDemandDepositAccountApiClient accountApiClient
	) {
		this.findAccountUserUseCase = findAccountUserUseCase;
		this.findUserAccountUseCase = findUserAccountUseCase;
		this.accountApiClient = accountApiClient;
	}

	public DemandDepositTransferDto.DepositResponse execute(Integer userId, AccountDto.DepositRequest request) {
		User user = findAccountUserUseCase.execute(userId);
		findUserAccountUseCase.execute(userId, request.accountNo());
		return accountApiClient.deposit(
			new DemandDepositTransferDto.DepositRequest(
				user.getUserKey(),
				request.accountNo(),
				request.transactionBalance(),
				request.transactionSummary()
			)
		);
	}
}

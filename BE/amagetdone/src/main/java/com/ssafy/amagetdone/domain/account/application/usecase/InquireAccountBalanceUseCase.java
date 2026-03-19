package com.ssafy.amagetdone.domain.account.application.usecase;

import com.ssafy.amagetdone.domain.account.infra.client.SsafyDemandDepositAccountApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositBalanceDto;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.springframework.stereotype.Component;

@Component
public class InquireAccountBalanceUseCase {

	private final FindAccountUserUseCase findAccountUserUseCase;
	private final FindUserAccountUseCase findUserAccountUseCase;
	private final SsafyDemandDepositAccountApiClient accountApiClient;

	public InquireAccountBalanceUseCase(
		FindAccountUserUseCase findAccountUserUseCase,
		FindUserAccountUseCase findUserAccountUseCase,
		SsafyDemandDepositAccountApiClient accountApiClient
	) {
		this.findAccountUserUseCase = findAccountUserUseCase;
		this.findUserAccountUseCase = findUserAccountUseCase;
		this.accountApiClient = accountApiClient;
	}

	public DemandDepositBalanceDto.Response execute(Integer userId, String accountNo) {
		User user = findAccountUserUseCase.execute(userId);
		findUserAccountUseCase.execute(userId, accountNo);
		return accountApiClient.inquireBalance(new DemandDepositBalanceDto.Request(user.getUserKey(), accountNo));
	}
}

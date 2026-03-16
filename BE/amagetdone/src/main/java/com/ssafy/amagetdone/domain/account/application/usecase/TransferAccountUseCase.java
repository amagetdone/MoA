package com.ssafy.amagetdone.domain.account.application.usecase;

import com.ssafy.amagetdone.domain.account.api.dto.AccountDto;
import com.ssafy.amagetdone.domain.account.infra.client.SsafyDemandDepositAccountApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositTransferDto;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.springframework.stereotype.Component;

@Component
public class TransferAccountUseCase {

	private final FindAccountUserUseCase findAccountUserUseCase;
	private final FindUserAccountUseCase findUserAccountUseCase;
	private final SsafyDemandDepositAccountApiClient accountApiClient;

	public TransferAccountUseCase(
		FindAccountUserUseCase findAccountUserUseCase,
		FindUserAccountUseCase findUserAccountUseCase,
		SsafyDemandDepositAccountApiClient accountApiClient
	) {
		this.findAccountUserUseCase = findAccountUserUseCase;
		this.findUserAccountUseCase = findUserAccountUseCase;
		this.accountApiClient = accountApiClient;
	}

	public DemandDepositTransferDto.TransferResponse execute(Integer userId, AccountDto.TransferRequest request) {
		User user = findAccountUserUseCase.execute(userId);
		findUserAccountUseCase.execute(userId, request.withdrawalAccountNo());
		return accountApiClient.transfer(
			new DemandDepositTransferDto.TransferRequest(
				user.getUserKey(),
				request.withdrawalAccountNo(),
				request.depositAccountNo(),
				request.transactionBalance(),
				request.withdrawalTransactionSummary(),
				request.depositTransactionSummary()
			)
		);
	}
}

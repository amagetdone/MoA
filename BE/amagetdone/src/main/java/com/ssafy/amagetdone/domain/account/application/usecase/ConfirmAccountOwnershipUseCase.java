package com.ssafy.amagetdone.domain.account.application.usecase;

import com.ssafy.amagetdone.domain.account.api.dto.AccountDto;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.infra.client.SsafyDemandDepositAccountApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositAccountDto;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.springframework.stereotype.Component;

@Component
public class ConfirmAccountOwnershipUseCase {

	private final FindAccountUserUseCase findAccountUserUseCase;
	private final SsafyDemandDepositAccountApiClient ssafyDemandDepositAccountApiClient;

	public ConfirmAccountOwnershipUseCase(
		FindAccountUserUseCase findAccountUserUseCase,
		SsafyDemandDepositAccountApiClient ssafyDemandDepositAccountApiClient
	) {
		this.findAccountUserUseCase = findAccountUserUseCase;
		this.ssafyDemandDepositAccountApiClient = ssafyDemandDepositAccountApiClient;
	}

	public AccountDto.ConfirmAccountResponse execute(Integer userId, AccountDto.ConfirmAccountRequest request) {
		User user = findAccountUserUseCase.execute(userId);

		try {
			DemandDepositAccountDto.AccountDetailResponse response = ssafyDemandDepositAccountApiClient.inquireAccount(
				new DemandDepositAccountDto.AccountDetailRequest(user.getUserKey(), request.accountNo())
			);
			return new AccountDto.ConfirmAccountResponse(request.accountNo().equals(response.accountNo()));
		} catch (AccountException exception) {
			return new AccountDto.ConfirmAccountResponse(false);
		}
	}
}

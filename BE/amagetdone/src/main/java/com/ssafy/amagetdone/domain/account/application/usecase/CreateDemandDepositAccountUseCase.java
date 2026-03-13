package com.ssafy.amagetdone.domain.account.application.usecase;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.account.infra.client.SsafyDemandDepositAccountApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositAccountDto;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CreateDemandDepositAccountUseCase {

	private final FindAccountUserUseCase findAccountUserUseCase;
	private final AccountRepository accountRepository;
	private final SsafyDemandDepositAccountApiClient accountApiClient;

	public CreateDemandDepositAccountUseCase(
		FindAccountUserUseCase findAccountUserUseCase,
		AccountRepository accountRepository,
		SsafyDemandDepositAccountApiClient accountApiClient
	) {
		this.findAccountUserUseCase = findAccountUserUseCase;
		this.accountRepository = accountRepository;
		this.accountApiClient = accountApiClient;
	}

	@Transactional
	public AccountAdminDto.AccountCreateResponse execute(AccountAdminDto.AccountCreateRequest request) {
		User user = findAccountUserUseCase.execute(request.userId());
		DemandDepositAccountDto.AccountCreateResponse response = accountApiClient.createAccount(
			new DemandDepositAccountDto.AccountCreateRequest(user.getUserKey(), request.accountTypeUniqueNo())
		);

		Account account = accountRepository.findByUserIdAndAccountNo(user.getId(), response.accountNo())
			.orElseGet(() -> Account.create(
				user,
				response.accountNo(),
				request.accountTypeUniqueNo(),
				null,
				null,
				response.bankCode(),
				response.bankCode(),
				response.currency()
			));
		account.sync(
			request.accountTypeUniqueNo(),
			null,
			null,
			response.bankCode(),
			response.bankCode(),
			response.currency()
		);
		accountRepository.save(account);

		return new AccountAdminDto.AccountCreateResponse(
			response.bankCode(),
			response.accountNo(),
			response.currency(),
			response.currencyName()
		);
	}
}

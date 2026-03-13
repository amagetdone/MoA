package com.ssafy.amagetdone.domain.account.application.usecase;

import java.util.ArrayList;
import java.util.List;

import com.ssafy.amagetdone.domain.account.infra.client.SsafyDemandDepositAccountApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositAccountDto;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SyncAccountsUseCase {

	private final FindAccountUserUseCase findAccountUserUseCase;
	private final AccountRepository accountRepository;
	private final SsafyDemandDepositAccountApiClient accountApiClient;

	public SyncAccountsUseCase(
		FindAccountUserUseCase findAccountUserUseCase,
		AccountRepository accountRepository,
		SsafyDemandDepositAccountApiClient accountApiClient
	) {
		this.findAccountUserUseCase = findAccountUserUseCase;
		this.accountRepository = accountRepository;
		this.accountApiClient = accountApiClient;
	}

	@Transactional
	public List<Account> execute(Integer userId) {
		User user = findAccountUserUseCase.execute(userId);
		DemandDepositAccountDto.AccountListResponse response = accountApiClient.inquireAccounts(
			new DemandDepositAccountDto.AccountListRequest(user.getUserKey())
		);

		List<Account> syncedAccounts = new ArrayList<>();
		for (DemandDepositAccountDto.AccountItem item : response.accounts()) {
			Account account = accountRepository.findByUserIdAndAccountNo(user.getId(), item.accountNo())
				.orElseGet(() -> Account.create(
					user,
					item.accountNo(),
					null,
					item.accountName(),
					item.accountTypeName(),
					item.bankCode(),
					item.bankName(),
					item.currency()
				));
			account.sync(
				null,
				item.accountName(),
				item.accountTypeName(),
				item.bankCode(),
				item.bankName(),
				item.currency()
			);
			syncedAccounts.add(accountRepository.save(account));
		}
		return syncedAccounts;
	}
}

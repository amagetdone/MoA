package com.ssafy.amagetdone.domain.account.application.usecase;

import java.util.List;

import com.ssafy.amagetdone.domain.account.api.dto.AccountDto;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;

import org.springframework.stereotype.Component;

@Component
public class ListAccountsUseCase {

	private final AccountRepository accountRepository;

	public ListAccountsUseCase(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	public List<Account> execute(Integer userId) {
		return accountRepository.findAllByUserId(userId);
	}

	public List<AccountDto.AccountSummaryResponse> toResponses(List<Account> accounts) {
		return accounts.stream()
			.map(account -> new AccountDto.AccountSummaryResponse(
				account.getAccountNo(),
				account.getBankCode(),
				account.getBankName(),
				account.getAccountName(),
				account.getAlias(),
				account.isVerified()
			))
			.toList();
	}
}

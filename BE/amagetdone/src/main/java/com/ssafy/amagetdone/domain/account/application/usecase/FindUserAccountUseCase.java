package com.ssafy.amagetdone.domain.account.application.usecase;

import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;

import org.springframework.stereotype.Component;

@Component
public class FindUserAccountUseCase {

	private final AccountRepository accountRepository;

	public FindUserAccountUseCase(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	public Account execute(Integer userId, String accountNo) {
		return accountRepository.findActiveLinkedAccountByUserIdAndAccountNo(userId, accountNo)
			.orElseThrow(() -> new AccountException(AccountErrorCode.ACCOUNT_NOT_FOUND));
	}
}

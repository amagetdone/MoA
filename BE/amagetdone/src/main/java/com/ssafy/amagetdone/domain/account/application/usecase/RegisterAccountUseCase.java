package com.ssafy.amagetdone.domain.account.application.usecase;

import com.ssafy.amagetdone.domain.account.api.dto.AccountDto;
import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Registers a user-provided bank account into the local DB for subsequent 1-won verification.
@Component
public class RegisterAccountUseCase {

	private final FindAccountUserUseCase findAccountUserUseCase;
	private final AccountRepository accountRepository;

	public RegisterAccountUseCase(
		FindAccountUserUseCase findAccountUserUseCase,
		AccountRepository accountRepository
	) {
		this.findAccountUserUseCase = findAccountUserUseCase;
		this.accountRepository = accountRepository;
	}

	@Transactional
	public AccountDto.AccountSummaryResponse execute(Integer userId, AccountDto.RegisterAccountRequest request) {
		User user = findAccountUserUseCase.execute(userId);

		Account account = accountRepository.findByUserIdAndAccountNo(userId, request.accountNo())
			.orElseGet(() -> Account.create(
				user,
				request.accountNo(),
				null,
				null,
				null,
				request.bankCode(),
				request.bankName(),
				null
			));
		account.sync(null, null, null, request.bankCode(), request.bankName(), null);
		accountRepository.save(account);

		return new AccountDto.AccountSummaryResponse(
			account.getAccountNo(),
			account.getBankCode(),
			account.getBankName(),
			account.getAccountName(),
			account.getAlias(),
			account.isVerified()
		);
	}
}

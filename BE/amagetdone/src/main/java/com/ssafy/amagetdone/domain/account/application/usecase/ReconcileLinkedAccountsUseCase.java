package com.ssafy.amagetdone.domain.account.application.usecase;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ssafy.amagetdone.domain.account.infra.client.SsafyDemandDepositAccountApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositAccountDto;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReconcileLinkedAccountsUseCase {

	private final FindAccountUserUseCase findAccountUserUseCase;
	private final AccountRepository accountRepository;
	private final SsafyDemandDepositAccountApiClient ssafyDemandDepositAccountApiClient;

	public ReconcileLinkedAccountsUseCase(
		FindAccountUserUseCase findAccountUserUseCase,
		AccountRepository accountRepository,
		SsafyDemandDepositAccountApiClient ssafyDemandDepositAccountApiClient
	) {
		this.findAccountUserUseCase = findAccountUserUseCase;
		this.accountRepository = accountRepository;
		this.ssafyDemandDepositAccountApiClient = ssafyDemandDepositAccountApiClient;
	}

	@Transactional
	public List<Account> execute(Integer userId) {
		List<Account> linkedAccounts = accountRepository.findAllLinkedAccountsByUserId(userId);
		if (linkedAccounts.isEmpty()) {
			return List.of();
		}
		User user = findAccountUserUseCase.execute(userId);

		Map<String, DemandDepositAccountDto.AccountItem> externalAccountsByNo = ssafyDemandDepositAccountApiClient.inquireAccounts(
			new DemandDepositAccountDto.AccountListRequest(user.getUserKey())
		).accounts().stream().collect(Collectors.toMap(
			DemandDepositAccountDto.AccountItem::accountNo,
			Function.identity(),
			(left, right) -> left
		));

		return linkedAccounts.stream()
			.filter(account -> reconcile(account, externalAccountsByNo))
			.toList();
	}

	private boolean reconcile(Account account, Map<String, DemandDepositAccountDto.AccountItem> externalAccountsByNo) {
		DemandDepositAccountDto.AccountItem externalAccount = externalAccountsByNo.get(account.getAccountNo());
		if (externalAccount == null) {
			account.markInactive();
			accountRepository.save(account);
			return false;
		}

		account.sync(
			null,
			externalAccount.accountName(),
			externalAccount.accountTypeName(),
			externalAccount.bankCode(),
			externalAccount.bankName(),
			externalAccount.currency()
		);
		account.markActive();
		return true;
	}
}

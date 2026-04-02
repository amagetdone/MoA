package com.ssafy.amagetdone.domain.account.application.usecase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.infra.client.SsafyDemandDepositAccountApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositAccountDto;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SyncUserAssetsUseCase {

	private static final Logger log = LoggerFactory.getLogger(SyncUserAssetsUseCase.class);

	private final FindAccountUserUseCase findAccountUserUseCase;
	private final AccountRepository accountRepository;
	private final SsafyDemandDepositAccountApiClient ssafyDemandDepositAccountApiClient;
	private final SyncLinkedCardsUseCase syncLinkedCardsUseCase;

	public SyncUserAssetsUseCase(
		FindAccountUserUseCase findAccountUserUseCase,
		AccountRepository accountRepository,
		SsafyDemandDepositAccountApiClient ssafyDemandDepositAccountApiClient,
		SyncLinkedCardsUseCase syncLinkedCardsUseCase
	) {
		this.findAccountUserUseCase = findAccountUserUseCase;
		this.accountRepository = accountRepository;
		this.ssafyDemandDepositAccountApiClient = ssafyDemandDepositAccountApiClient;
		this.syncLinkedCardsUseCase = syncLinkedCardsUseCase;
	}

	@Transactional
	public void execute(Integer userId) {
		User user = findAccountUserUseCase.execute(userId);
		String userKey = requireUserKey(user);

		List<Account> localAccounts = accountRepository.findAllByUserId(userId);
		Map<String, Account> localAccountsByNo = localAccounts.stream()
			.collect(Collectors.toMap(Account::getAccountNo, Function.identity(), (left, right) -> left));
		List<DemandDepositAccountDto.AccountItem> remoteAccounts = ssafyDemandDepositAccountApiClient.inquireAccounts(
			new DemandDepositAccountDto.AccountListRequest(userKey)
		).accounts();

		Set<String> remoteAccountNos = new HashSet<>();
		List<Account> activeLinkedAccounts = new ArrayList<>();
		for (DemandDepositAccountDto.AccountItem remoteAccount : remoteAccounts) {
			remoteAccountNos.add(remoteAccount.accountNo());
			Account synchronizedAccount = upsertLinkedAccount(user, localAccountsByNo.get(remoteAccount.accountNo()), remoteAccount);
			activeLinkedAccounts.add(synchronizedAccount);
		}

		for (Account localAccount : localAccounts) {
			if (remoteAccountNos.contains(localAccount.getAccountNo()) || !localAccount.isActive()) {
				continue;
			}
			localAccount.markInactive();
			accountRepository.save(localAccount);
		}

		for (Account account : activeLinkedAccounts) {
			syncCardsSafely(userId, userKey, account);
		}
	}

	private String requireUserKey(User user) {
		String userKey = user.getUserKey();
		if (!hasText(userKey)) {
			throw new AccountException(AccountErrorCode.USER_KEY_REQUIRED);
		}
		return userKey;
	}

	private Account upsertLinkedAccount(
		User user,
		Account existingAccount,
		DemandDepositAccountDto.AccountItem remoteAccount
	) {
		Account account = existingAccount;
		if (account == null) {
			account = Account.create(
				user,
				remoteAccount.accountNo(),
				null,
				remoteAccount.accountName(),
				remoteAccount.accountTypeName(),
				remoteAccount.bankCode(),
				remoteAccount.bankName(),
				remoteAccount.currency()
			);
		} else {
			account.sync(
				null,
				remoteAccount.accountName(),
				remoteAccount.accountTypeName(),
				remoteAccount.bankCode(),
				remoteAccount.bankName(),
				remoteAccount.currency()
			);
		}
		account.markVerifiedLinked();
		return accountRepository.save(account);
	}

	private void syncCardsSafely(Integer userId, String userKey, Account account) {
		try {
			syncLinkedCardsUseCase.execute(userId, userKey, account.getAccountNo());
		} catch (RuntimeException exception) {
			log.warn(
				"Asset sync skipped linked card sync. userId={}, accountNo={}, reason={}",
				userId,
				account.getAccountNo(),
				exception.getClass().getSimpleName()
			);
		}
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}

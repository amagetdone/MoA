package com.ssafy.amagetdone.domain.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;

@DataJpaTest
@Import(ListAccountsUseCase.class)
class ListAccountsUseCaseTest {

	@Autowired
	private ListAccountsUseCase listAccountsUseCase;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void executeIncludesActiveUnverifiedAccountsAndKeepsInactiveAccountsExcluded() {
		User user = persistUser("active-user@example.com");

		Account activeUnverified = persistAccount(user, "111-111-111111", "신한은행", false, true);
		Account activeVerified = persistAccount(user, "222-222-222222", "국민은행", true, true);
		persistAccount(user, "333-333-333333", "하나은행", false, false);

		entityManager.flush();
		entityManager.clear();

		List<Account> result = listAccountsUseCase.execute(user.getId());

		assertThat(result)
			.extracting(Account::getAccountNo, Account::isVerified, Account::isActive)
			.containsExactly(
				org.assertj.core.groups.Tuple.tuple(activeUnverified.getAccountNo(), false, true),
				org.assertj.core.groups.Tuple.tuple(activeVerified.getAccountNo(), true, true)
			);
	}

	private User persistUser(String email) {
		User user = User.createPendingSocialUser();
		user.applyTestSocialProfile("tester", email);
		user.activate();
		entityManager.persist(user);
		return user;
	}

	private Account persistAccount(User user, String accountNo, String bankName, boolean verified, boolean active) {
		Account account = Account.create(
			user,
			accountNo,
			"DEMAND",
			"테스트 통장",
			"수시입출금",
			"001",
			bankName,
			"KRW"
		);
		if (verified) {
			account.markVerified();
		}
		if (!active) {
			account.markInactive();
		}
		return accountRepository.save(account);
	}
}

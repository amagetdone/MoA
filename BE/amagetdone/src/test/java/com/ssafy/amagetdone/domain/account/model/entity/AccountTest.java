// Responsibility: Verifies account entity state transitions for local linked-account lifecycle rules.
package com.ssafy.amagetdone.domain.account.model.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.junit.jupiter.api.Test;

class AccountTest {

	@Test
	void newAccountStartsUnverifiedUntilExplicitVerification() {
		User user = User.createPendingSocialUser();
		user.updateUserKey("user-key");
		Account account = Account.create(
			user,
			"0011234567890123",
			"001-1-abc",
			"데모 계좌",
			"수시입출금",
			"001",
			"한국은행",
			"KRW"
		);

		assertFalse(account.isVerified());
		assertTrue(account.isActive());

		account.markVerified();

		assertTrue(account.isVerified());
	}

	@Test
	void syncDoesNotAutoVerifyAccount() {
		User user = User.createPendingSocialUser();
		Account account = Account.create(
			user,
			"0011234567890123",
			"001-1-abc",
			"데모 계좌",
			"수시입출금",
			"001",
			"한국은행",
			"KRW"
		);

		account.sync(
			"001-1-abc",
			"데모 계좌",
			"수시입출금",
			"001",
			"한국은행",
			"KRW"
		);

		assertFalse(account.isVerified());
	}

	@Test
	void inactiveAccountCanBeReactivatedWhenLinkedAgain() {
		User user = User.createPendingSocialUser();
		Account account = Account.create(
			user,
			"0011234567890123",
			"001-1-abc",
			"데모 계좌",
			"수시입출금",
			"001",
			"한국은행",
			"KRW"
		);

		account.markInactive();
		assertFalse(account.isActive());

		account.markActive();
		assertTrue(account.isActive());
	}

	@Test
	void softDeleteMarksAccountUnverifiedInactiveAndDeleted() {
		User user = User.createPendingSocialUser();
		Account account = Account.create(
			user,
			"0011234567890123",
			"001-1-abc",
			"데모 계좌",
			"수시입출금",
			"001",
			"한국은행",
			"KRW"
		);
		account.markVerified();
		account.markActive();

		account.softDelete();

		assertFalse(account.isVerified());
		assertFalse(account.isActive());
		assertTrue(account.isDeleted());
	}

	@Test
	void verifiedRelinkRestoresDeletedAccount() {
		User user = User.createPendingSocialUser();
		Account account = Account.create(
			user,
			"0011234567890123",
			"001-1-abc",
			"데모 계좌",
			"수시입출금",
			"001",
			"한국은행",
			"KRW"
		);
		account.softDelete();

		account.markVerifiedLinked();

		assertTrue(account.isVerified());
		assertTrue(account.isActive());
		assertFalse(account.isDeleted());
	}
}

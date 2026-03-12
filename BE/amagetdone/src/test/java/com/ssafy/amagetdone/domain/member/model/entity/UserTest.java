package com.ssafy.amagetdone.domain.member.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class UserTest {

	@Test
	void pendingSocialUserStartsWithoutFinancialUserKey() {
		User user = User.createPendingSocialUser();

		assertEquals("TEMP", user.getStatus().name());
		assertNull(user.getUserKey());
	}

	@Test
	void markingProfileDoesNotActivateUserUntilExplicitActivation() {
		User user = User.createPendingSocialUser();

		user.markProfileEntered("moa", "moa@example.com", LocalDate.of(1997, 3, 1), true);

		assertTrue(user.isProfileCompleted());
		assertEquals("TEMP", user.getStatus().name());

		user.activate();

		assertEquals("ACTIVE", user.getStatus().name());
	}
}

package com.ssafy.amagetdone.domain.account.support.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;

import org.junit.jupiter.api.Test;

class LocalCardUniqueNoMapperTest {

	private final LocalCardUniqueNoMapper mapper = new LocalCardUniqueNoMapper();

	@Test
	void toLocalIdReturnsSameValueForSameRemoteId() {
		Long first = mapper.toLocalId("1005-992db475fbb944c");
		Long second = mapper.toLocalId("1005-992db475fbb944c");

		assertThat(first).isEqualTo(second);
		assertThat(first).isPositive();
	}

	@Test
	void toLocalIdRejectsBlankRemoteId() {
		assertThatThrownBy(() -> mapper.toLocalId(" "))
			.isInstanceOf(AccountException.class)
			.extracting("errorCode")
			.isEqualTo(AccountErrorCode.INVALID_LINKED_CARD_UNIQUE_NO);
	}
}

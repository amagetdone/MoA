package com.ssafy.amagetdone.domain.account.support.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;

import org.junit.jupiter.api.Test;

class LocalCardUniqueNoMapperTest {

	private final LocalCardUniqueNoMapper mapper = new LocalCardUniqueNoMapper();

	@Test
	void toLocalIdReturnsSameValueForSameCardNo() {
		Long first = mapper.toLocalId("1005518816096479");
		Long second = mapper.toLocalId("1005518816096479");

		assertThat(first).isEqualTo(second);
		assertThat(first).isPositive();
	}

	@Test
	void toLocalIdReturnsDifferentValueForDifferentCardNumbers() {
		Long first = mapper.toLocalId("1005518816096479");
		Long second = mapper.toLocalId("1005518816096480");

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void toLocalIdRejectsBlankRemoteId() {
		assertThatThrownBy(() -> mapper.toLocalId(" "))
			.isInstanceOf(AccountException.class)
			.extracting("errorCode")
			.isEqualTo(AccountErrorCode.INVALID_LINKED_CARD_UNIQUE_NO);
	}
}

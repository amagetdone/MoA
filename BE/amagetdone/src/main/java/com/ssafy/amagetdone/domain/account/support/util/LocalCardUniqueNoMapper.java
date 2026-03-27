package com.ssafy.amagetdone.domain.account.support.util;

import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;

import org.springframework.stereotype.Component;

@Component
public class LocalCardUniqueNoMapper {

	public Long toLocalId(String cardNo) {
		if (cardNo == null || cardNo.isBlank()) {
			throw new AccountException(AccountErrorCode.INVALID_LINKED_CARD_UNIQUE_NO);
		}

		long hashed = Math.abs((long)cardNo.trim().hashCode());
		return hashed == 0 ? 1L : hashed;
	}
}

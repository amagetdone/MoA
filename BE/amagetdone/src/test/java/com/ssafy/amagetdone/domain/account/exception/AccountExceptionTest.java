package com.ssafy.amagetdone.domain.account.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.ssafy.amagetdone.global.infra.ssafy.exception.SsafyApiException;

import org.junit.jupiter.api.Test;

class AccountExceptionTest {

	@Test
	void extractsSsafyResponseDetailFromCause() {
		AccountException exception = new AccountException(
			AccountErrorCode.DEMAND_DEPOSIT_ACCOUNT_API_CALL_FAILED,
			new SsafyApiException(SsafyApiException.Reason.FAILED_RESPONSE_CODE, "H1008", "API_KEY가 유효하지 않습니다.")
		);

		assertThat(exception.getDetail()).isEqualTo(Map.of(
			"source", "ssafy",
			"reason", "FAILED_RESPONSE_CODE",
			"responseCode", "H1008",
			"responseMessage", "API_KEY가 유효하지 않습니다."
		));
	}
}

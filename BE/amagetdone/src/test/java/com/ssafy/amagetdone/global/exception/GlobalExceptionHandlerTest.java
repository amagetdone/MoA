package com.ssafy.amagetdone.global.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.global.infra.ssafy.exception.SsafyApiException;
import com.ssafy.amagetdone.global.response.ErrorResponse;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

	@Test
	void handleAccountExceptionIncludesUpstreamDetailWhenPresent() {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();
		AccountException exception = new AccountException(
			AccountErrorCode.DEMAND_DEPOSIT_ACCOUNT_API_CALL_FAILED,
			new SsafyApiException(SsafyApiException.Reason.FAILED_RESPONSE_CODE, "A5001", "invalid KST time")
		);

		ResponseEntity<ErrorResponse> response = handler.handleAccountException(exception);

		assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
		assertEquals("error", response.getBody().status());
		assertEquals(AccountErrorCode.DEMAND_DEPOSIT_ACCOUNT_API_CALL_FAILED.message(), response.getBody().message());
		assertEquals(AccountErrorCode.DEMAND_DEPOSIT_ACCOUNT_API_CALL_FAILED.code(), response.getBody().errorCode());
		assertEquals("ssafy", response.getBody().detail().get("source"));
		assertEquals("A5001", response.getBody().detail().get("responseCode"));
		assertEquals("invalid KST time", response.getBody().detail().get("responseMessage"));
	}
}

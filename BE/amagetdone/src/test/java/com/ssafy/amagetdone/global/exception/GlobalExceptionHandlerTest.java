package com.ssafy.amagetdone.global.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ssafy.amagetdone.domain.accountverification.exception.AccountVerificationErrorCode;
import com.ssafy.amagetdone.domain.accountverification.exception.AccountVerificationException;
import com.ssafy.amagetdone.domain.plan.exception.PlanErrorCode;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.global.response.ErrorResponse;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

	@Test
	void handleAccountVerificationExceptionReturnsMappedErrorResponse() {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();

		ResponseEntity<ErrorResponse> response = handler.handleAccountVerificationException(
			new AccountVerificationException(AccountVerificationErrorCode.AUTH_CODE_MISMATCH)
		);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("error", response.getBody().status());
		assertEquals(AccountVerificationErrorCode.AUTH_CODE_MISMATCH.message(), response.getBody().message());
		assertEquals(AccountVerificationErrorCode.AUTH_CODE_MISMATCH.code(), response.getBody().errorCode());
	}

	@Test
	void handlePlanExceptionReturnsMappedErrorResponse() {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();

		ResponseEntity<ErrorResponse> response = handler.handlePlanException(
			new PlanException(PlanErrorCode.PLAN_NOT_FOUND)
		);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals("error", response.getBody().status());
		assertEquals(PlanErrorCode.PLAN_NOT_FOUND.message(), response.getBody().message());
		assertEquals(PlanErrorCode.PLAN_NOT_FOUND.code(), response.getBody().errorCode());
	}
}

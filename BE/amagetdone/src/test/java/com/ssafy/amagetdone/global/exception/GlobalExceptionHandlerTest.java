package com.ssafy.amagetdone.global.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.accountverification.exception.AccountVerificationErrorCode;
import com.ssafy.amagetdone.domain.accountverification.exception.AccountVerificationException;
import com.ssafy.amagetdone.domain.plan.exception.PlanErrorCode;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.global.infra.ssafy.exception.SsafyApiException;
import com.ssafy.amagetdone.global.response.ErrorResponse;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

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

	@Test
	void handleMethodArgumentNotValidExceptionReturnsBadRequest() throws NoSuchMethodException {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
		bindingResult.addError(new FieldError("request", "dateRangeValid", false, false, null, null,
			"startDate must be on or before endDate"));
		MethodParameter methodParameter = new MethodParameter(
			SampleTarget.class.getDeclaredMethod("sample", String.class),
			0
		);
		MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

		ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValidException(exception);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("error", response.getBody().status());
		assertEquals("INVALID_REQUEST", response.getBody().errorCode());
		assertEquals("Validation failed", response.getBody().message());
		assertEquals(Map.of("dateRangeValid", "startDate must be on or before endDate"), response.getBody().detail());
	}

	@Test
	void handleDataIntegrityViolationExceptionReturnsConflictWithConstraintDetail() {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();
		DataIntegrityViolationException exception = new DataIntegrityViolationException(
			"could not execute statement",
			new RuntimeException("ERROR: new row for relation \"plan\" violates check constraint \"plan_status_check\"")
		);

		ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolationException(exception);

		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertEquals("error", response.getBody().status());
		assertEquals("DATA_INTEGRITY_VIOLATION", response.getBody().errorCode());
		assertEquals("Data integrity violation", response.getBody().message());
		assertEquals("plan_status_check", response.getBody().detail().get("constraint"));
	}

	private static class SampleTarget {
		@SuppressWarnings("unused")
		void sample(String value) {
		}
	}
}

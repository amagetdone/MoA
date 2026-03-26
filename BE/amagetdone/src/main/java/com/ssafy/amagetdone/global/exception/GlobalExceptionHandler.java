package com.ssafy.amagetdone.global.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.accountverification.exception.AccountVerificationException;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.global.response.ErrorResponse;
import com.ssafy.amagetdone.domain.transaction.exception.TransactionException;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MemberException.class)
	public ResponseEntity<ErrorResponse> handleMemberException(MemberException exception) {
		return ResponseEntity.status(exception.getErrorCode().httpStatus())
			.body(ErrorResponse.from(exception.getErrorCode()));
	}

	@ExceptionHandler(AccountException.class)
	public ResponseEntity<ErrorResponse> handleAccountException(AccountException exception) {
		return ResponseEntity.status(exception.getErrorCode().httpStatus())
			.body(ErrorResponse.from(exception.getErrorCode(), exception.getDetail()));
	}

	@ExceptionHandler(AccountVerificationException.class)
	public ResponseEntity<ErrorResponse> handleAccountVerificationException(AccountVerificationException exception) {
		return ResponseEntity.status(exception.getErrorCode().httpStatus())
			.body(ErrorResponse.from(exception.getErrorCode()));
	}

	@ExceptionHandler(PlanException.class)
	public ResponseEntity<ErrorResponse> handlePlanException(PlanException exception) {
		return ResponseEntity.status(exception.getErrorCode().httpStatus())
			.body(ErrorResponse.from(exception.getErrorCode()));
	}

	@ExceptionHandler(TransactionException.class)
	public ResponseEntity<ErrorResponse> handleTransactionException(TransactionException exception) {
		return ResponseEntity.status(exception.getErrorCode().httpStatus())
			.body(ErrorResponse.from(exception.getErrorCode()));
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException exception) {
		HttpStatusCode statusCode = exception.getStatusCode();
		return ResponseEntity.status(statusCode)
			.body(ErrorResponse.of(exception.getReason(), "RESPONSE_STATUS_ERROR"));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ErrorResponse.of(exception.getMessage(), "INVALID_REQUEST"));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
		Map<String, Object> detail = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors()
			.forEach(fieldError -> detail.put(fieldError.getField(), fieldError.getDefaultMessage()));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ErrorResponse.of("Validation failed", "INVALID_REQUEST", detail));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ErrorResponse.of("Malformed request body", "INVALID_REQUEST"));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException exception) {
		Map<String, Object> detail = new LinkedHashMap<>();
		String message = exception.getMostSpecificCause() != null
			? exception.getMostSpecificCause().getMessage()
			: exception.getMessage();
		if (message != null && message.contains("constraint")) {
			int keywordIndex = message.indexOf("constraint");
			int start = keywordIndex >= 0 ? message.indexOf('"', keywordIndex) : -1;
			int end = start >= 0 ? message.indexOf('"', start + 1) : -1;
			if (start >= 0 && end > start) {
				detail.put("constraint", message.substring(start + 1, end));
			}
		}
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(ErrorResponse.of("Data integrity violation", "DATA_INTEGRITY_VIOLATION", detail.isEmpty() ? null : detail));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception exception) {
		log.error("Unhandled exception", exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ErrorResponse.of("Internal server error", "INTERNAL_SERVER_ERROR"));
	}
}

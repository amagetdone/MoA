package com.ssafy.amagetdone.global.exception;

import com.ssafy.amagetdone.domain.asset.exception.AssetException;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.transaction.exception.TransactionException;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MemberException.class)
	public ResponseEntity<ErrorResponse> handleMemberException(MemberException exception) {
		return ResponseEntity.status(exception.getErrorCode().httpStatus())
			.body(ErrorResponse.from(exception.getErrorCode()));
	}

	@ExceptionHandler(AssetException.class)
	public ResponseEntity<ErrorResponse> handleAssetException(AssetException exception) {
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
			.body(ErrorResponse.of(statusCode.value(), "RESPONSE_STATUS_ERROR", exception.getReason()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "INVALID_REQUEST", exception.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception exception) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ErrorResponse.of(
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				"INTERNAL_SERVER_ERROR",
				"Internal server error"
			));
	}
}

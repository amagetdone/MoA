package com.ssafy.amagetdone.domain.transaction.exception;

import com.ssafy.amagetdone.global.exception.ErrorCode;

import org.springframework.http.HttpStatus;

public enum TransactionErrorCode implements ErrorCode {
	CARD_TRANSACTION_API_CALL_FAILED(HttpStatus.BAD_GATEWAY, "TRANSACTION_001", "Card transaction API call failed"),
	INVALID_CARD_TRANSACTION_API_RESPONSE(HttpStatus.BAD_GATEWAY, "TRANSACTION_002",
		"Invalid card transaction API response"),
	DEMAND_DEPOSIT_TRANSACTION_API_CALL_FAILED(HttpStatus.BAD_GATEWAY, "TRANSACTION_003",
		"Demand deposit transaction API call failed"),
	INVALID_DEMAND_DEPOSIT_API_RESPONSE(HttpStatus.BAD_GATEWAY, "TRANSACTION_004",
		"Invalid demand deposit transaction API response"),
	MERCHANT_INFERENCE_API_CALL_FAILED(HttpStatus.BAD_GATEWAY, "TRANSACTION_005",
		"Merchant inference API call failed"),
	INVALID_MERCHANT_INFERENCE_API_RESPONSE(HttpStatus.BAD_GATEWAY, "TRANSACTION_006",
		"Invalid merchant inference API response"),
	TRANSACTION_ANALYSIS_ALREADY_RUNNING(HttpStatus.CONFLICT, "TRANSACTION_007",
		"Transaction analysis is already running"),
	TRANSACTION_ANALYSIS_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "TRANSACTION_008",
		"Transaction analysis job was not found"),
	TRANSACTION_ANALYSIS_PROCESS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "TRANSACTION_009",
		"Transaction analysis process failed"),
	TRANSACTION_ANALYSIS_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "TRANSACTION_010",
		"Transaction analysis result was not found");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	TransactionErrorCode(HttpStatus httpStatus, String code, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}

	@Override
	public String code() {
		return code;
	}

	@Override
	public String message() {
		return message;
	}

	@Override
	public HttpStatus httpStatus() {
		return httpStatus;
	}
}

package com.ssafy.amagetdone.domain.account.exception;

import com.ssafy.amagetdone.global.exception.ErrorCode;

import org.springframework.http.HttpStatus;

public enum AccountErrorCode implements ErrorCode {
	ACCOUNT_ERROR(HttpStatus.BAD_REQUEST, "ACCOUNT_001", "Account error"),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT_002", "User not found"),
	USER_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "ACCOUNT_003", "User key is required"),
	ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT_004", "Account not found"),
	CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT_020", "Card not found"),
	INVALID_DATA_INIT_REQUEST(HttpStatus.BAD_REQUEST, "ACCOUNT_019", "Invalid data init request"),
	TRANSACTION_INIT_SEED_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "ACCOUNT_021", "Transaction init seed not found"),
	DEMAND_DEPOSIT_PRODUCT_API_CALL_FAILED(
		HttpStatus.BAD_GATEWAY,
		"ACCOUNT_005",
		"Demand deposit product API call failed"
	),
	INVALID_DEMAND_DEPOSIT_PRODUCT_API_RESPONSE(
		HttpStatus.BAD_GATEWAY,
		"ACCOUNT_006",
		"Invalid demand deposit product API response"
	),
	DEMAND_DEPOSIT_ACCOUNT_API_CALL_FAILED(
		HttpStatus.BAD_GATEWAY,
		"ACCOUNT_007",
		"Demand deposit account API call failed"
	),
	INVALID_DEMAND_DEPOSIT_ACCOUNT_API_RESPONSE(
		HttpStatus.BAD_GATEWAY,
		"ACCOUNT_008",
		"Invalid demand deposit account API response"
	),
	DEMAND_DEPOSIT_BALANCE_API_CALL_FAILED(
		HttpStatus.BAD_GATEWAY,
		"ACCOUNT_009",
		"Demand deposit balance API call failed"
	),
	INVALID_DEMAND_DEPOSIT_BALANCE_API_RESPONSE(
		HttpStatus.BAD_GATEWAY,
		"ACCOUNT_010",
		"Invalid demand deposit balance API response"
	),
	DEMAND_DEPOSIT_DEPOSIT_API_CALL_FAILED(
		HttpStatus.BAD_GATEWAY,
		"ACCOUNT_011",
		"Demand deposit deposit API call failed"
	),
	INVALID_DEMAND_DEPOSIT_DEPOSIT_API_RESPONSE(
		HttpStatus.BAD_GATEWAY,
		"ACCOUNT_012",
		"Invalid demand deposit deposit API response"
	),
	DEMAND_DEPOSIT_TRANSFER_API_CALL_FAILED(
		HttpStatus.BAD_GATEWAY,
		"ACCOUNT_013",
		"Demand deposit transfer API call failed"
	),
	INVALID_DEMAND_DEPOSIT_TRANSFER_API_RESPONSE(
		HttpStatus.BAD_GATEWAY,
		"ACCOUNT_014",
		"Invalid demand deposit transfer API response"
	),
	CREDIT_CARD_API_CALL_FAILED(
		HttpStatus.BAD_GATEWAY,
		"ACCOUNT_015",
		"Credit card API call failed"
	),
	INVALID_CREDIT_CARD_API_RESPONSE(
		HttpStatus.BAD_GATEWAY,
		"ACCOUNT_016",
		"Invalid credit card API response"
	),
	CREDIT_CARD_TRANSACTION_API_CALL_FAILED(
		HttpStatus.BAD_GATEWAY,
		"ACCOUNT_017",
		"Credit card transaction API call failed"
	),
	INVALID_CREDIT_CARD_TRANSACTION_API_RESPONSE(
		HttpStatus.BAD_GATEWAY,
		"ACCOUNT_018",
		"Invalid credit card transaction API response"
	),
	CREDIT_CARD_TRANSACTION_CANCEL_API_CALL_FAILED(
		HttpStatus.BAD_GATEWAY,
		"ACCOUNT_027",
		"Credit card transaction cancel API call failed"
	),
	INVALID_CREDIT_CARD_TRANSACTION_CANCEL_API_RESPONSE(
		HttpStatus.BAD_GATEWAY,
		"ACCOUNT_028",
		"Invalid credit card transaction cancel API response"
	),
	LINKED_CARD_QUERY_API_CALL_FAILED(
		HttpStatus.BAD_GATEWAY,
		"ACCOUNT_022",
		"Linked card query API call failed"
	),
	INVALID_LINKED_CARD_QUERY_API_RESPONSE(
		HttpStatus.BAD_GATEWAY,
		"ACCOUNT_023",
		"Invalid linked card query API response"
	),
	LINKED_ACCOUNT_CARD_NOT_FOUND(
		HttpStatus.BAD_GATEWAY,
		"ACCOUNT_024",
		"Linked account card not found"
	),
	LINKED_CARD_SYNC_FAILED(
		HttpStatus.BAD_GATEWAY,
		"ACCOUNT_025",
		"Linked card sync failed"
	),
	INVALID_LINKED_CARD_UNIQUE_NO(
		HttpStatus.BAD_REQUEST,
		"ACCOUNT_026",
		"Invalid linked card unique number"
	);

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	AccountErrorCode(HttpStatus httpStatus, String code, String message) {
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

package com.ssafy.amagetdone.domain.member.exception;

import com.ssafy.amagetdone.global.exception.ErrorCode;

import org.springframework.http.HttpStatus;

public enum MemberErrorCode implements ErrorCode {
	UNSUPPORTED_SOCIAL_PROVIDER(HttpStatus.BAD_REQUEST, "MEMBER_001", "Unsupported social provider"),
	GOOGLE_ID_TOKEN_REQUIRED(HttpStatus.BAD_REQUEST, "MEMBER_002", "Google id token is required"),
	INVALID_GOOGLE_ID_TOKEN(HttpStatus.UNAUTHORIZED, "MEMBER_003", "Invalid Google id token"),
	KAKAO_ACCESS_TOKEN_REQUIRED(HttpStatus.BAD_REQUEST, "MEMBER_005", "Kakao access token is required"),
	INVALID_KAKAO_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "MEMBER_006", "Invalid Kakao access token"),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_007", "User not found"),
	AUTHORIZATION_HEADER_REQUIRED(HttpStatus.UNAUTHORIZED, "MEMBER_008", "Authorization header is required"),
	ACCESS_TOKEN_REQUIRED(HttpStatus.UNAUTHORIZED, "MEMBER_009", "Access token is required"),
	INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "MEMBER_010", "Invalid access token"),
	REFRESH_TOKEN_REQUIRED(HttpStatus.BAD_REQUEST, "MEMBER_011", "Refresh token is required"),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "MEMBER_012", "Invalid refresh token"),
	REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "MEMBER_013", "Refresh token reuse detected"),
	FINANCIAL_MEMBER_LINK_FAILED(HttpStatus.BAD_GATEWAY, "MEMBER_014", "Financial member link failed"),
	FINANCIAL_MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "MEMBER_015", "Financial member already exists"),
	FINANCIAL_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_016", "Financial member not found"),
	FINANCIAL_MEMBER_CREATE_FAILED(HttpStatus.BAD_GATEWAY, "MEMBER_017", "Financial member create failed"),
	FINANCIAL_MEMBER_SEARCH_FAILED(HttpStatus.BAD_GATEWAY, "MEMBER_018", "Financial member search failed"),
	FINANCIAL_API_KEY_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "MEMBER_019", "Financial api key is missing"),
	FINANCIAL_MEMBER_ALREADY_LINKED(HttpStatus.CONFLICT, "MEMBER_020", "Financial member already linked to another user");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	MemberErrorCode(HttpStatus httpStatus, String code, String message) {
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

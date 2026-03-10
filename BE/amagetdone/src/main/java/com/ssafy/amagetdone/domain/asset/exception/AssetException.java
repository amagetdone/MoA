package com.ssafy.amagetdone.domain.asset.exception;

public class AssetException extends RuntimeException {

	private final AssetErrorCode errorCode;

	public AssetException(AssetErrorCode errorCode) {
		super(errorCode.message());
		this.errorCode = errorCode;
	}

	public AssetException(AssetErrorCode errorCode, Throwable cause) {
		super(errorCode.message(), cause);
		this.errorCode = errorCode;
	}

	public AssetErrorCode getErrorCode() {
		return errorCode;
	}
}

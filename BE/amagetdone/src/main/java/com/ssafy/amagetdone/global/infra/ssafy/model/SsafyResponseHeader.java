package com.ssafy.amagetdone.global.infra.ssafy.model;

public record SsafyResponseHeader(
	String responseCode,
	String responseMessage,
	String apiName,
	String transmissionDate,
	String transmissionTime,
	String institutionCode,
	String fintechAppNo,
	String apiServiceCode,
	String institutionTransactionUniqueNo
) {
}

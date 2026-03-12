package com.ssafy.amagetdone.global.infra.ssafy.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SsafyRequestHeader(
	String apiName,
	String transmissionDate,
	String transmissionTime,
	String institutionCode,
	String fintechAppNo,
	String apiServiceCode,
	String institutionTransactionUniqueNo,
	String apiKey,
	String userKey
) {
}

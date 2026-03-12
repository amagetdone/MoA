package com.ssafy.amagetdone.global.infra.ssafy.model;

public record SsafyResponse<T>(
	SsafyResponseHeader header,
	T payload
) {
}

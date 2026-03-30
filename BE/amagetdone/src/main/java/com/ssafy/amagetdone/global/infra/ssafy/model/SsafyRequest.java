package com.ssafy.amagetdone.global.infra.ssafy.model;

public record SsafyRequest<T>(
	SsafyRequestHeader header,
	T payload
) {
}

package com.ssafy.amagetdone.global.infra.ssafy.envelope;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.ssafy.amagetdone.global.infra.ssafy.model.SsafyRequest;
import com.ssafy.amagetdone.global.infra.ssafy.model.SsafyRequestHeader;

public class SsafyRequestEnvelope<T> {

	@JsonProperty("Header")
	private final SsafyRequestHeader header;

	@JsonUnwrapped
	private final T payload;

	public SsafyRequestEnvelope(
		@JsonProperty("Header") SsafyRequestHeader header,
		@JsonUnwrapped T payload
	) {
		this.header = header;
		this.payload = payload;
	}

	public static <T> SsafyRequestEnvelope<T> from(SsafyRequest<T> request) {
		return new SsafyRequestEnvelope<>(request.header(), request.payload());
	}

	public SsafyRequestHeader header() {
		return header;
	}

	public T payload() {
		return payload;
	}
}

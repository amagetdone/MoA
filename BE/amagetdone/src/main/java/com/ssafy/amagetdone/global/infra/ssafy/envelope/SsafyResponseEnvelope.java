package com.ssafy.amagetdone.global.infra.ssafy.envelope;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.amagetdone.global.infra.ssafy.model.SsafyResponse;
import com.ssafy.amagetdone.global.infra.ssafy.model.SsafyResponseHeader;

public class SsafyResponseEnvelope<T> {

	private final SsafyResponseHeader header;
	private final T payload;

	@JsonCreator
	public SsafyResponseEnvelope(
		@JsonProperty("Header") SsafyResponseHeader header,
		@JsonProperty("REC") T payload
	) {
		this.header = header;
		this.payload = payload;
	}

	@JsonProperty("Header")
	public SsafyResponseHeader header() {
		return header;
	}

	@JsonProperty("REC")
	public T payload() {
		return payload;
	}

	public SsafyResponse<T> toResponse() {
		return new SsafyResponse<>(header, payload);
	}
}

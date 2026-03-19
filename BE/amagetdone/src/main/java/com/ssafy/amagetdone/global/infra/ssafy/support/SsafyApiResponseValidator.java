package com.ssafy.amagetdone.global.infra.ssafy.support;

import com.ssafy.amagetdone.global.infra.ssafy.envelope.SsafyResponseEnvelope;
import com.ssafy.amagetdone.global.infra.ssafy.exception.SsafyApiException;
import com.ssafy.amagetdone.global.infra.ssafy.model.SsafyResponse;

import org.springframework.stereotype.Component;

@Component
public class SsafyApiResponseValidator {

	private static final String SUCCESS_CODE = "H0000";

	public <T> SsafyResponse<T> validate(SsafyResponseEnvelope<T> envelope) {
		if (envelope == null || envelope.header() == null) {
			throw new SsafyApiException(SsafyApiException.Reason.INVALID_HEADER);
		}
		if (!SUCCESS_CODE.equals(envelope.header().responseCode())) {
			throw new SsafyApiException(
				SsafyApiException.Reason.FAILED_RESPONSE_CODE,
				envelope.header().responseCode(),
				envelope.header().responseMessage()
			);
		}
		if (envelope.payload() == null) {
			throw new SsafyApiException(SsafyApiException.Reason.INVALID_PAYLOAD);
		}
		return envelope.toResponse();
	}
}

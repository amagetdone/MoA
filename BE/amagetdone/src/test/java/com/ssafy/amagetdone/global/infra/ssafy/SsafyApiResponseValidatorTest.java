package com.ssafy.amagetdone.global.infra.ssafy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ssafy.amagetdone.global.infra.ssafy.envelope.SsafyResponseEnvelope;
import com.ssafy.amagetdone.global.infra.ssafy.exception.SsafyApiException;
import com.ssafy.amagetdone.global.infra.ssafy.model.SsafyResponse;
import com.ssafy.amagetdone.global.infra.ssafy.model.SsafyResponseHeader;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiResponseValidator;

import org.junit.jupiter.api.Test;

class SsafyApiResponseValidatorTest {

	@Test
	void validateReturnsResponseWhenHeaderAndPayloadAreValid() {
		SsafyApiResponseValidator validator = new SsafyApiResponseValidator();

		SsafyResponse<TestPayload> response = validator.validate(
			new SsafyResponseEnvelope<>(
				new SsafyResponseHeader(
					"H0000",
					"OK",
					"inquireMerchantList",
					"20260312",
					"101010",
					"00100",
					"001",
					"inquireMerchantList",
					"20260312101010000001"
				),
				new TestPayload("merchant")
			)
		);

		assertEquals("H0000", response.header().responseCode());
		assertEquals("merchant", response.payload().value());
	}

	@Test
	void validateThrowsWhenResponseCodeIsNotSuccess() {
		SsafyApiResponseValidator validator = new SsafyApiResponseValidator();

		assertThrows(
			SsafyApiException.class,
			() -> validator.validate(
				new SsafyResponseEnvelope<>(
					new SsafyResponseHeader(
						"H1000",
						"invalid",
						"inquireMerchantList",
						"20260312",
						"101010",
						"00100",
						"001",
						"inquireMerchantList",
						"20260312101010000001"
					),
					new TestPayload("merchant")
				)
			)
		);
	}

	@Test
	void validateThrowsWhenPayloadIsMissing() {
		SsafyApiResponseValidator validator = new SsafyApiResponseValidator();

		assertThrows(
			SsafyApiException.class,
			() -> validator.validate(
				new SsafyResponseEnvelope<>(
					new SsafyResponseHeader(
						"H0000",
						"OK",
						"inquireMerchantList",
						"20260312",
						"101010",
						"00100",
						"001",
						"inquireMerchantList",
						"20260312101010000001"
					),
					null
				)
			)
		);
	}

	private record TestPayload(String value) {
	}
}

package com.ssafy.amagetdone.global.infra.ssafy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.ssafy.amagetdone.global.infra.ssafy.envelope.SsafyRequestEnvelope;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiRequestFactory;

import org.junit.jupiter.api.Test;

class SsafyApiRequestFactoryTest {

	@Test
	void createWithoutUserKeyBuildsCommonHeader() {
		SsafyApiRequestFactory factory = new SsafyApiRequestFactory();

		SsafyRequestEnvelope<TestPayload> request = factory.create(
			"inquireMerchantList",
			"00100",
			"001",
			"test-api-key",
			null,
			new TestPayload("merchant")
		);

		assertEquals("inquireMerchantList", request.header().apiName());
		assertEquals("00100", request.header().institutionCode());
		assertEquals("001", request.header().fintechAppNo());
		assertEquals("test-api-key", request.header().apiKey());
		assertNull(request.header().userKey());
		assertNotNull(request.header().institutionTransactionUniqueNo());
		assertEquals("merchant", request.payload().value());
	}

	@Test
	void createWithUserKeyBuildsHeaderContainingUserKey() {
		SsafyApiRequestFactory factory = new SsafyApiRequestFactory();

		SsafyRequestEnvelope<TestPayload> request = factory.create(
			"inquireCreditCardTransactionList",
			"00100",
			"001",
			"test-api-key",
			"user-key",
			new TestPayload("card")
		);

		assertEquals("inquireCreditCardTransactionList", request.header().apiName());
		assertEquals("user-key", request.header().userKey());
		assertEquals("card", request.payload().value());
	}

	@Test
	void createBuildsTransmissionDateTimeInKst() {
		Clock fixedUtcClock = Clock.fixed(Instant.parse("2026-03-14T15:30:45Z"), ZoneOffset.UTC);
		SsafyApiRequestFactory factory = new SsafyApiRequestFactory(fixedUtcClock);

		SsafyRequestEnvelope<TestPayload> request = factory.create(
			"inquireDemandDepositAccountList",
			"00100",
			"001",
			"test-api-key",
			"user-key",
			new TestPayload("deposit")
		);

		assertEquals("20260315", request.header().transmissionDate());
		assertEquals("003045", request.header().transmissionTime());
	}

	private record TestPayload(String value) {
	}
}

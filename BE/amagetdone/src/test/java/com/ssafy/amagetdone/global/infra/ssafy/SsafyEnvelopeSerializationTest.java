package com.ssafy.amagetdone.global.infra.ssafy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.amagetdone.global.infra.ssafy.envelope.SsafyRequestEnvelope;
import com.ssafy.amagetdone.global.infra.ssafy.envelope.SsafyResponseEnvelope;
import com.ssafy.amagetdone.global.infra.ssafy.model.SsafyRequestHeader;

import org.junit.jupiter.api.Test;

class SsafyEnvelopeSerializationTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void requestEnvelopeSerializesHeaderAndPayloadAsFlatJson() throws Exception {
		SsafyRequestEnvelope<TestPayload> envelope = new SsafyRequestEnvelope<>(
			new SsafyRequestHeader(
				"inquireCreditCardTransactionList",
				"20260312",
				"101010",
				"00100",
				"001",
				"inquireCreditCardTransactionList",
				"20260312101010000001",
				"api-key",
				"user-key"
			),
			new TestPayload("1234567890123456", "725")
		);

		String json = objectMapper.writeValueAsString(envelope);

		assertTrue(json.contains("\"Header\""));
		assertTrue(json.contains("\"cardNo\":\"1234567890123456\""));
		assertTrue(json.contains("\"cvc\":\"725\""));
	}

	@Test
	void responseEnvelopeDeserializesHeaderAndPayloadFromRecJson() throws Exception {
		String json = """
			{
			  "Header": {
			    "responseCode": "H0000",
			    "responseMessage": "OK",
			    "apiName": "inquireMerchantList",
			    "transmissionDate": "20260312",
			    "transmissionTime": "101010",
			    "institutionCode": "00100",
			    "fintechAppNo": "001",
			    "apiServiceCode": "inquireMerchantList",
			    "institutionTransactionUniqueNo": "20260312101010000001"
			  },
			  "REC": {
			    "categoryId": "CG-1",
			    "merchantName": "coffee"
			  }
			}
			""";

		SsafyResponseEnvelope<TestResponsePayload> envelope = objectMapper.readValue(
			json,
			objectMapper.getTypeFactory().constructParametricType(SsafyResponseEnvelope.class, TestResponsePayload.class)
		);

		assertEquals("H0000", envelope.header().responseCode());
		assertEquals("CG-1", envelope.payload().categoryId());
		assertEquals("coffee", envelope.payload().merchantName());
	}

	private record TestPayload(String cardNo, String cvc) {
	}

	private record TestResponsePayload(String categoryId, String merchantName) {
	}
}

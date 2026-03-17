package com.ssafy.amagetdone.domain.transaction.infra.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import com.ssafy.amagetdone.domain.transaction.exception.TransactionException;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MerchantInferenceApiClientTest {

	@Test
	void labelMerchantsMapsResponse() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		MerchantInferenceApiProperties properties = new MerchantInferenceApiProperties();
		properties.setBaseUrl("https://j14d101.p.ssafy.io/inference");
		MerchantInferenceApiClient client = new MerchantInferenceApiClient(builder, properties);

		server.expect(requestTo("https://j14d101.p.ssafy.io/inference/v1/merchant/batch"))
			.andExpect(method(POST))
			.andExpect(content().contentType(APPLICATION_JSON))
			.andExpect(content().string(containsString("\"merchant_texts\":[\"스타벅스 강남점\",\"(수시입출금) : 출금\"]")))
			.andRespond(withSuccess("""
				{
				  "results": [
				    {
				      "merchant_text": "스타벅스 강남점",
				      "normalized_merchant_text": "스타벅스 강남점",
				      "raw_label": "카페",
				      "final_label": "카페",
				      "confidence": 0.97,
				      "fallback_applied": false,
				      "threshold_used": 0.5
				    },
				    {
				      "merchant_text": "(수시입출금) : 출금",
				      "normalized_merchant_text": "출금",
				      "raw_label": "기타",
				      "final_label": "기타",
				      "confidence": 0.41,
				      "fallback_applied": true,
				      "threshold_used": 0.5
				    }
				  ]
				}
				""", APPLICATION_JSON));

		var result = client.labelMerchants(List.of("스타벅스 강남점", "(수시입출금) : 출금"));

		assertEquals(2, result.results().size());
		assertEquals("카페", result.results().get(0).finalLabel());
		assertEquals("출금", result.results().get(1).normalizedMerchantText());
		assertEquals(true, result.results().get(1).fallbackApplied());
	}

	@Test
	void labelMerchantsThrowsWhenResponseBodyIsInvalid() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		MerchantInferenceApiProperties properties = new MerchantInferenceApiProperties();
		properties.setBaseUrl("https://j14d101.p.ssafy.io/inference");
		MerchantInferenceApiClient client = new MerchantInferenceApiClient(builder, properties);

		server.expect(requestTo("https://j14d101.p.ssafy.io/inference/v1/merchant/batch"))
			.andExpect(method(POST))
			.andRespond(withSuccess("""
				{
				  "results": null
				}
				""", APPLICATION_JSON));

		assertThrows(TransactionException.class, () -> client.labelMerchants(List.of("스타벅스 강남점")));
	}

	@Test
	void labelMerchantsThrowsWhenTransportFails() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		MerchantInferenceApiProperties properties = new MerchantInferenceApiProperties();
		properties.setBaseUrl("https://j14d101.p.ssafy.io/inference");
		MerchantInferenceApiClient client = new MerchantInferenceApiClient(builder, properties);

		server.expect(requestTo("https://j14d101.p.ssafy.io/inference/v1/merchant/batch"))
			.andExpect(method(POST))
			.andRespond(withServerError());

		assertThrows(TransactionException.class, () -> client.labelMerchants(List.of("스타벅스 강남점")));
	}
}

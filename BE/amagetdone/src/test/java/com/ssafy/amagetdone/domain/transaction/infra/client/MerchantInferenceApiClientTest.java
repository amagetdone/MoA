package com.ssafy.amagetdone.domain.transaction.infra.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.ssafy.amagetdone.domain.transaction.infra.client.dto.MerchantInferenceDto;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MerchantInferenceApiClientTest {

	private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
		MediaType.APPLICATION_JSON,
		StandardCharsets.UTF_8
	);

	@Test
	void labelMerchantsSendsUtf8JsonContentTypeAndKoreanPayload() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		MerchantInferenceApiProperties properties = new MerchantInferenceApiProperties();
		properties.setBaseUrl("https://j14d101.p.ssafy.io/inference");
		MerchantInferenceApiClient client = new MerchantInferenceApiClient(builder, properties);

		server.expect(requestTo("https://j14d101.p.ssafy.io/inference/v1/merchant/batch"))
			.andExpect(method(POST))
			.andExpect(content().contentType(APPLICATION_JSON_UTF8))
			.andExpect(content().json("""
				{
				  "merchant_texts": ["카카오T택시"]
				}
				"""))
			.andRespond(withSuccess("""
				{
				  "results": [
				    {
				      "merchant_text": "카카오T택시",
				      "normalized_merchant_text": "카카오t택시",
				      "raw_label": "교통비",
				      "final_label": "교통비",
				      "confidence": 0.81,
				      "fallback_applied": false,
				      "threshold_used": 0.7
				    }
				  ]
				}
				""", MediaType.APPLICATION_JSON));

		MerchantInferenceDto.Response response = client.labelMerchants(List.of("카카오T택시"));

		assertThat(response.results()).hasSize(1);
		assertThat(response.results().get(0).finalLabel()).isEqualTo("교통비");
	}
}

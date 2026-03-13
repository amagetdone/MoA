package com.ssafy.amagetdone.domain.asset.infra.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ssafy.amagetdone.domain.asset.exception.AssetException;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiRequestFactory;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiResponseValidator;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SsafyMerchantApiClientTest {

	@Test
	void inquireMerchantListMapsResponse() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		AssetSsafyApiProperties properties = new AssetSsafyApiProperties();
		properties.setBaseUrl("https://finopenapi.ssafy.io/ssafy/api/v1/edu");
		properties.setInstitutionCode("00100");
		properties.setFintechAppNo("001");
		SsafyMerchantApiClient client = new SsafyMerchantApiClient(
			builder,
			properties,
			new SsafyApiRequestFactory(),
			new SsafyApiResponseValidator()
		);
		ReflectionTestUtils.setField(client, "apiKey", "asset-api-key");

		server.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireMerchantList"))
			.andExpect(method(POST))
			.andExpect(content().string(containsString("\"Header\"")))
			.andExpect(content().string(containsString("\"apiName\":\"inquireMerchantList\"")))
			.andExpect(content().string(not(containsString("\"userKey\""))))
			.andRespond(withSuccess("""
				{
				  "Header": {
				    "responseCode": "H0000",
				    "responseMessage": "정상처리 되었습니다."
				  },
				  "REC": [
				    {
				      "categoryId": "CG-1",
				      "categoryName": "생활",
				      "merchantId": 7,
				      "merchantName": "스타벅스"
				    },
				    {
				      "categoryId": "CG-2",
				      "categoryName": "교통",
				      "merchantId": 8,
				      "merchantName": "지하철"
				    }
				  ]
				}
				""", APPLICATION_JSON));

		MerchantListResult result = client.inquireMerchantList();

		assertEquals(2, result.merchants().size());
		assertEquals("CG-1", result.merchants().get(0).categoryId());
		assertEquals("스타벅스", result.merchants().get(0).merchantName());
		assertEquals(8L, result.merchants().get(1).merchantId());
	}

	@Test
	void inquireMerchantListThrowsWhenResponseCodeIsNotSuccess() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		AssetSsafyApiProperties properties = new AssetSsafyApiProperties();
		properties.setBaseUrl("https://finopenapi.ssafy.io/ssafy/api/v1/edu");
		properties.setInstitutionCode("00100");
		properties.setFintechAppNo("001");
		SsafyMerchantApiClient client = new SsafyMerchantApiClient(
			builder,
			properties,
			new SsafyApiRequestFactory(),
			new SsafyApiResponseValidator()
		);
		ReflectionTestUtils.setField(client, "apiKey", "asset-api-key");

		server.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireMerchantList"))
			.andExpect(method(POST))
			.andRespond(withSuccess("""
				{
				  "Header": {
				    "responseCode": "A1042",
				    "responseMessage": "가맹점 조회 실패"
				  },
				  "REC": []
				}
				""", APPLICATION_JSON));

		assertThrows(AssetException.class, client::inquireMerchantList);
	}

	@Test
	void inquireMerchantListThrowsWhenTransportFails() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		AssetSsafyApiProperties properties = new AssetSsafyApiProperties();
		properties.setBaseUrl("https://finopenapi.ssafy.io/ssafy/api/v1/edu");
		properties.setInstitutionCode("00100");
		properties.setFintechAppNo("001");
		SsafyMerchantApiClient client = new SsafyMerchantApiClient(
			builder,
			properties,
			new SsafyApiRequestFactory(),
			new SsafyApiResponseValidator()
		);
		ReflectionTestUtils.setField(client, "apiKey", "asset-api-key");

		server.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireMerchantList"))
			.andExpect(method(POST))
			.andRespond(withServerError());

		assertThrows(AssetException.class, client::inquireMerchantList);
	}
}

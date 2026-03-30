package com.ssafy.amagetdone.domain.account.infra.client;

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

import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.infra.client.dto.MerchantQueryDto;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiRequestFactory;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiResponseValidator;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SsafyMerchantQueryApiClientTest {

	@Test
	void inquireMerchantListMapsResponse() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		AccountSsafyApiProperties properties = new AccountSsafyApiProperties();
		properties.setBaseUrl("https://finopenapi.ssafy.io/ssafy/api/v1/edu");
		properties.setInstitutionCode("00100");
		properties.setFintechAppNo("001");
		SsafyMerchantQueryApiClient client = new SsafyMerchantQueryApiClient(
			builder,
			properties,
			new SsafyApiRequestFactory(),
			new SsafyApiResponseValidator()
		);
		ReflectionTestUtils.setField(client, "apiKey", "account-api-key");

		server.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireMerchantList"))
			.andExpect(method(POST))
			.andExpect(content().string(containsString("\"apiName\":\"inquireMerchantList\"")))
			.andExpect(content().string(containsString("\"apiServiceCode\":\"inquireMerchantList\"")))
			.andExpect(content().string(containsString("\"apiKey\":\"account-api-key\"")))
			.andRespond(withSuccess("""
				{
				  "Header": {
				    "responseCode": "H0000",
				    "responseMessage": "정상처리 되었습니다."
				  },
				  "REC": [
				    {
				      "categoryId": "CG-traffic",
				      "categoryName": "교통",
				      "merchantId": 7,
				      "merchantName": "UT 택시"
				    },
				    {
				      "categoryId": "CG-cafe",
				      "categoryName": "카페",
				      "merchantId": 8,
				      "merchantName": "스타벅스"
				    }
				  ]
				}
				""", APPLICATION_JSON));

		MerchantQueryDto.Response result = client.inquireMerchantList();

		assertEquals(2, result.merchants().size());
		assertEquals(7L, result.merchants().get(0).merchantId());
		assertEquals("UT 택시", result.merchants().get(0).merchantName());
	}

	@Test
	void inquireMerchantListThrowsWhenResponseCodeIsNotSuccess() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		AccountSsafyApiProperties properties = new AccountSsafyApiProperties();
		properties.setBaseUrl("https://finopenapi.ssafy.io/ssafy/api/v1/edu");
		properties.setInstitutionCode("00100");
		properties.setFintechAppNo("001");
		SsafyMerchantQueryApiClient client = new SsafyMerchantQueryApiClient(
			builder,
			properties,
			new SsafyApiRequestFactory(),
			new SsafyApiResponseValidator()
		);
		ReflectionTestUtils.setField(client, "apiKey", "account-api-key");

		server.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireMerchantList"))
			.andExpect(method(POST))
			.andRespond(withSuccess("""
				{
				  "Header": {
				    "responseCode": "A1056",
				    "responseMessage": "가맹점 ID가 유효하지 않습니다."
				  },
				  "REC": null
				}
				""", APPLICATION_JSON));

		assertThrows(AccountException.class, client::inquireMerchantList);
	}

	@Test
	void inquireMerchantListThrowsWhenTransportFails() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		AccountSsafyApiProperties properties = new AccountSsafyApiProperties();
		properties.setBaseUrl("https://finopenapi.ssafy.io/ssafy/api/v1/edu");
		properties.setInstitutionCode("00100");
		properties.setFintechAppNo("001");
		SsafyMerchantQueryApiClient client = new SsafyMerchantQueryApiClient(
			builder,
			properties,
			new SsafyApiRequestFactory(),
			new SsafyApiResponseValidator()
		);
		ReflectionTestUtils.setField(client, "apiKey", "account-api-key");

		server.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireMerchantList"))
			.andExpect(method(POST))
			.andRespond(withServerError());

		assertThrows(AccountException.class, client::inquireMerchantList);
	}
}

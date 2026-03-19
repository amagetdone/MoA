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
import com.ssafy.amagetdone.domain.account.infra.client.dto.CreditCardQueryDto;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiRequestFactory;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiResponseValidator;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SsafyCreditCardQueryApiClientTest {

	@Test
	void inquireLinkedCardsMapsResponse() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		AccountSsafyApiProperties properties = new AccountSsafyApiProperties();
		properties.setBaseUrl("https://finopenapi.ssafy.io/ssafy/api/v1/edu");
		properties.setInstitutionCode("00100");
		properties.setFintechAppNo("001");
		SsafyCreditCardQueryApiClient client = new SsafyCreditCardQueryApiClient(
			builder,
			properties,
			new SsafyApiRequestFactory(),
			new SsafyApiResponseValidator()
		);
		ReflectionTestUtils.setField(client, "apiKey", "account-api-key");

		server.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireSignUpCreditCardList"))
			.andExpect(method(POST))
			.andExpect(content().string(containsString("\"apiName\":\"inquireSignUpCreditCardList\"")))
			.andExpect(content().string(containsString("\"apiServiceCode\":\"inquireSignUpCreditCardList\"")))
			.andExpect(content().string(containsString("\"apiKey\":\"account-api-key\"")))
			.andExpect(content().string(containsString("\"userKey\":\"user-key\"")))
			.andRespond(withSuccess("""
				{
				  "Header": {
				    "responseCode": "H0000",
				    "responseMessage": "정상처리 되었습니다."
				  },
				  "REC": [
				    {
				      "cardNo": "1005518816096479",
				      "cvc": "725",
				      "cardUniqueNo": "1005-992db475fbb944c",
				      "cardIssuerCode": "1005",
				      "cardIssuerName": "신한카드",
				      "cardName": "신한 TRAVEL 카드",
				      "baselinePerformance": 0,
				      "maxBenefitLimit": 200000,
				      "cardDescription": "생활 20% 할인",
				      "cardExpiryDate": "20290409",
				      "withdrawalAccountNo": "0011234567890123",
				      "withdrawalDate": "4"
				    }
				  ]
				}
				""", APPLICATION_JSON));

		CreditCardQueryDto.Response result = client.inquireLinkedCards(new CreditCardQueryDto.Request("user-key"));

		assertEquals(1, result.cards().size());
		assertEquals("1005-992db475fbb944c", result.cards().get(0).cardUniqueNo());
		assertEquals("0011234567890123", result.cards().get(0).withdrawalAccountNo());
		assertEquals("725", result.cards().get(0).cvc());
	}

	@Test
	void inquireLinkedCardsThrowsWhenResponseCodeIsNotSuccess() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		AccountSsafyApiProperties properties = new AccountSsafyApiProperties();
		properties.setBaseUrl("https://finopenapi.ssafy.io/ssafy/api/v1/edu");
		properties.setInstitutionCode("00100");
		properties.setFintechAppNo("001");
		SsafyCreditCardQueryApiClient client = new SsafyCreditCardQueryApiClient(
			builder,
			properties,
			new SsafyApiRequestFactory(),
			new SsafyApiResponseValidator()
		);
		ReflectionTestUtils.setField(client, "apiKey", "account-api-key");

		server.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireSignUpCreditCardList"))
			.andExpect(method(POST))
			.andRespond(withSuccess("""
				{
				  "Header": {
				    "responseCode": "A1054",
				    "responseMessage": "카드번호가 유효하지 않습니다."
				  },
				  "REC": null
				}
				""", APPLICATION_JSON));

		assertThrows(AccountException.class, () -> client.inquireLinkedCards(new CreditCardQueryDto.Request("user-key")));
	}

	@Test
	void inquireLinkedCardsThrowsWhenTransportFails() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		AccountSsafyApiProperties properties = new AccountSsafyApiProperties();
		properties.setBaseUrl("https://finopenapi.ssafy.io/ssafy/api/v1/edu");
		properties.setInstitutionCode("00100");
		properties.setFintechAppNo("001");
		SsafyCreditCardQueryApiClient client = new SsafyCreditCardQueryApiClient(
			builder,
			properties,
			new SsafyApiRequestFactory(),
			new SsafyApiResponseValidator()
		);
		ReflectionTestUtils.setField(client, "apiKey", "account-api-key");

		server.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireSignUpCreditCardList"))
			.andExpect(method(POST))
			.andRespond(withServerError());

		assertThrows(AccountException.class, () -> client.inquireLinkedCards(new CreditCardQueryDto.Request("user-key")));
	}
}

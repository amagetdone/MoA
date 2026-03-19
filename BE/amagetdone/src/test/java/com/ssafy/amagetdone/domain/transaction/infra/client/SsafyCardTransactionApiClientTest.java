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

import com.ssafy.amagetdone.domain.transaction.exception.TransactionException;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.CardTransactionDto;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiRequestFactory;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiResponseValidator;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SsafyCardTransactionApiClientTest {

	@Test
	void inquireCardTransactionsMapsResponse() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		TransactionSsafyApiProperties properties = new TransactionSsafyApiProperties();
		properties.setBaseUrl("https://finopenapi.ssafy.io/ssafy/api/v1/edu");
		properties.setInstitutionCode("00100");
		properties.setFintechAppNo("001");
		SsafyCardTransactionApiClient client = new SsafyCardTransactionApiClient(
			builder,
			properties,
			new SsafyApiRequestFactory(),
			new SsafyApiResponseValidator()
		);
		ReflectionTestUtils.setField(client, "apiKey", "transaction-api-key");

		server.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireCreditCardTransactionList"))
			.andExpect(method(POST))
			.andExpect(content().string(containsString("\"Header\"")))
			.andExpect(content().string(containsString("\"userKey\":\"user-key\"")))
			.andExpect(content().string(containsString("\"cardNo\":\"1005518816096479\"")))
			.andRespond(withSuccess("""
				{
				  "Header": {
				    "responseCode": "H0000",
				    "responseMessage": "정상처리 되었습니다."
				  },
				  "REC": {
				    "cardIssuerCode": "1005",
				    "cardIssuerName": "신한카드",
				    "cardName": "신한 TRAVEL 카드",
				    "cardNo": "1005518816096479",
				    "estimatedBalance": 2000000,
				    "transactionList": [
				      {
				        "transactionUniqueNo": 20,
				        "categoryId": "CG-3fa85f6425e811e",
				        "categoryName": "주유",
				        "merchantId": 1,
				        "merchantName": "SK 에너지",
				        "transactionDate": "20240418",
				        "transactionTime": "094431",
				        "transactionBalance": 1000000,
				        "cardStatus": "승인",
				        "billStatementsYn": "N",
				        "billStatementsStatus": "미결제"
				      }
				    ]
				  }
				}
				""", APPLICATION_JSON));

		CardTransactionDto.Response result = client.inquireTransactions(
			new CardTransactionDto.Request("user-key", "1005518816096479", "725", "20240401", "20240502")
		);

		assertEquals("1005", result.cardIssuerCode());
		assertEquals("신한카드", result.cardIssuerName());
		assertEquals(1, result.transactions().size());
		assertEquals(20L, result.transactions().get(0).transactionUniqueNo());
		assertEquals("SK 에너지", result.transactions().get(0).merchantName());
		assertEquals("미결제", result.transactions().get(0).billStatementsStatus());
	}

	@Test
	void inquireCardTransactionsThrowsWhenResponseCodeIsNotSuccess() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		TransactionSsafyApiProperties properties = new TransactionSsafyApiProperties();
		properties.setBaseUrl("https://finopenapi.ssafy.io/ssafy/api/v1/edu");
		properties.setInstitutionCode("00100");
		properties.setFintechAppNo("001");
		SsafyCardTransactionApiClient client = new SsafyCardTransactionApiClient(
			builder,
			properties,
			new SsafyApiRequestFactory(),
			new SsafyApiResponseValidator()
		);
		ReflectionTestUtils.setField(client, "apiKey", "transaction-api-key");

		server.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireCreditCardTransactionList"))
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

		assertThrows(
			TransactionException.class,
			() -> client.inquireTransactions(
				new CardTransactionDto.Request("user-key", "1005518816096479", "725", "20240401", "20240502")
			)
		);
	}

	@Test
	void inquireCardTransactionsThrowsWhenTransportFails() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		TransactionSsafyApiProperties properties = new TransactionSsafyApiProperties();
		properties.setBaseUrl("https://finopenapi.ssafy.io/ssafy/api/v1/edu");
		properties.setInstitutionCode("00100");
		properties.setFintechAppNo("001");
		SsafyCardTransactionApiClient client = new SsafyCardTransactionApiClient(
			builder,
			properties,
			new SsafyApiRequestFactory(),
			new SsafyApiResponseValidator()
		);
		ReflectionTestUtils.setField(client, "apiKey", "transaction-api-key");

		server.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireCreditCardTransactionList"))
			.andExpect(method(POST))
			.andRespond(withServerError());

		assertThrows(
			TransactionException.class,
			() -> client.inquireTransactions(
				new CardTransactionDto.Request("user-key", "1005518816096479", "725", "20240401", "20240502")
			)
		);
	}
}

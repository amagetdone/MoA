package com.ssafy.amagetdone.domain.transaction.infra.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ssafy.amagetdone.domain.transaction.exception.TransactionException;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.DemandDepositTransactionDto;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiRequestFactory;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiResponseValidator;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SsafyDemandDepositTransactionApiClientTest {

	@Test
	void inquireTransactionsMapsResponse() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		TransactionSsafyApiProperties properties = new TransactionSsafyApiProperties();
		properties.setBaseUrl("https://finopenapi.ssafy.io/ssafy/api/v1/edu");
		properties.setInstitutionCode("00100");
		properties.setFintechAppNo("001");
		SsafyDemandDepositTransactionApiClient client = new SsafyDemandDepositTransactionApiClient(
			builder,
			properties,
			new SsafyApiRequestFactory(),
			new SsafyApiResponseValidator()
		);
		ReflectionTestUtils.setField(client, "apiKey", "transaction-api-key");

		server.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireTransactionHistoryList"))
			.andExpect(method(POST))
			.andExpect(content().string(containsString("\"Header\"")))
			.andExpect(content().string(containsString("\"userKey\":\"user-key\"")))
			.andExpect(content().string(containsString("\"accountNo\":\"0016174648358792\"")))
			.andRespond(withSuccess("""
				{
				  "Header": {
				    "responseCode": "H0000",
				    "responseMessage": "정상처리 되었습니다."
				  },
				  "REC": {
				    "totalCount": 2,
				    "list": [
				      {
				        "transactionUniqueNo": 59,
				        "transactionDate": "20240401",
				        "transactionTime": "102447",
				        "transactionType": "1",
				        "transactionTypeName": "입금",
				        "transactionAccountNo": "",
				        "transactionBalance": 100000000,
				        "transactionAfterBalance": 100000000,
				        "transactionSummary": "(수시입출금) : 입금",
				        "transactionMemo": ""
				      }
				    ]
				  }
				}
				""", APPLICATION_JSON));

		DemandDepositTransactionDto.Response result = client.inquireTransactions(
			new DemandDepositTransactionDto.Request(
				"user-key",
				"0016174648358792",
				"20240101",
				"20241231",
				"A",
				"ASC"
			)
		);

		assertEquals(2, result.totalCount());
		assertEquals(1, result.transactions().size());
		assertEquals(59L, result.transactions().get(0).transactionUniqueNo());
		assertEquals("입금", result.transactions().get(0).transactionTypeName());
		assertEquals(100000000L, result.transactions().get(0).transactionAfterBalance());
	}

	@Test
	void inquireTransactionsThrowsWhenResponseCodeIsNotSuccess() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		TransactionSsafyApiProperties properties = new TransactionSsafyApiProperties();
		properties.setBaseUrl("https://finopenapi.ssafy.io/ssafy/api/v1/edu");
		properties.setInstitutionCode("00100");
		properties.setFintechAppNo("001");
		SsafyDemandDepositTransactionApiClient client = new SsafyDemandDepositTransactionApiClient(
			builder,
			properties,
			new SsafyApiRequestFactory(),
			new SsafyApiResponseValidator()
		);
		ReflectionTestUtils.setField(client, "apiKey", "transaction-api-key");

		server.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireTransactionHistoryList"))
			.andExpect(method(POST))
			.andRespond(withSuccess("""
				{
				  "Header": {
				    "responseCode": "A1003",
				    "responseMessage": "계좌번호가 유효하지 않습니다."
				  },
				  "REC": null
				}
				""", APPLICATION_JSON));

		assertThrows(
			TransactionException.class,
			() -> client.inquireTransactions(
				new DemandDepositTransactionDto.Request(
					"user-key",
					"0016174648358792",
					"20240101",
					"20241231",
					"A",
					"ASC"
				)
			)
		);
	}

	@Test
	void inquireTransactionsThrowsWhenListIsMissing() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		TransactionSsafyApiProperties properties = new TransactionSsafyApiProperties();
		properties.setBaseUrl("https://finopenapi.ssafy.io/ssafy/api/v1/edu");
		properties.setInstitutionCode("00100");
		properties.setFintechAppNo("001");
		SsafyDemandDepositTransactionApiClient client = new SsafyDemandDepositTransactionApiClient(
			builder,
			properties,
			new SsafyApiRequestFactory(),
			new SsafyApiResponseValidator()
		);
		ReflectionTestUtils.setField(client, "apiKey", "transaction-api-key");

		server.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireTransactionHistoryList"))
			.andExpect(method(POST))
			.andRespond(withSuccess("""
				{
				  "Header": {
				    "responseCode": "H0000",
				    "responseMessage": "정상처리 되었습니다."
				  },
				  "REC": {
				    "totalCount": 0
				  }
				}
				""", APPLICATION_JSON));

		assertThrows(
			TransactionException.class,
			() -> client.inquireTransactions(
				new DemandDepositTransactionDto.Request(
					"user-key",
					"0016174648358792",
					"20240101",
					"20241231",
					"A",
					"ASC"
				)
			)
		);
	}
}

package com.ssafy.amagetdone.domain.accountverification.infra.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ssafy.amagetdone.domain.accountverification.exception.AccountVerificationErrorCode;
import com.ssafy.amagetdone.domain.accountverification.exception.AccountVerificationException;
import com.ssafy.amagetdone.domain.accountverification.infra.client.dto.AccountVerificationClientDto;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiRequestFactory;
import com.ssafy.amagetdone.global.infra.ssafy.support.SsafyApiResponseValidator;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SsafyAccountVerificationApiClientTest {

	@Test
	void requestVerificationCallsOpenAccountAuthEndpoint() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		AccountVerificationSsafyApiProperties properties = new AccountVerificationSsafyApiProperties();
		properties.setBaseUrl("https://finopenapi.ssafy.io/ssafy/api/v1/edu");
		properties.setInstitutionCode("00100");
		properties.setFintechAppNo("001");
		SsafyAccountVerificationApiClient client = new SsafyAccountVerificationApiClient(
			builder,
			properties,
			new SsafyApiRequestFactory(),
			new SsafyApiResponseValidator()
		);
		ReflectionTestUtils.setField(client, "apiKey", "verification-api-key");

		server.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/accountAuth/openAccountAuth"))
			.andExpect(method(POST))
			.andExpect(content().string(containsString("\"userKey\":\"user-key\"")))
			.andExpect(content().string(containsString("\"accountNo\":\"0011234567890123\"")))
			.andExpect(content().string(containsString("\"authText\":\"AMAGETDONE\"")))
			.andRespond(withSuccess("""
				{
				  "Header": {
				    "responseCode": "H0000",
				    "responseMessage": "정상처리 되었습니다."
				  },
				  "REC": {
				    "transactionUniqueNo": 88,
				    "accountNo": "0011234567890123"
				  }
				}
				""", APPLICATION_JSON));

		AccountVerificationClientDto.RequestVerificationResponse result = client.requestVerification(
			new AccountVerificationClientDto.RequestVerificationRequest(
				"user-key",
				"0011234567890123",
				"AMAGETDONE"
			)
		);

		assertThat(result.transactionUniqueNo()).isEqualTo(88L);
		assertThat(result.accountNo()).isEqualTo("0011234567890123");
	}

	@Test
	void checkVerificationCallsCheckAuthCodeEndpoint() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		AccountVerificationSsafyApiProperties properties = new AccountVerificationSsafyApiProperties();
		properties.setBaseUrl("https://finopenapi.ssafy.io/ssafy/api/v1/edu");
		properties.setInstitutionCode("00100");
		properties.setFintechAppNo("001");
		SsafyAccountVerificationApiClient client = new SsafyAccountVerificationApiClient(
			builder,
			properties,
			new SsafyApiRequestFactory(),
			new SsafyApiResponseValidator()
		);
		ReflectionTestUtils.setField(client, "apiKey", "verification-api-key");

		server.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/accountAuth/checkAuthCode"))
			.andExpect(method(POST))
			.andExpect(content().string(containsString("\"authCode\":\"1234\"")))
			.andExpect(content().string(containsString("\"authText\":\"AMAGETDONE\"")))
			.andRespond(withSuccess("""
				{
				  "Header": {
				    "responseCode": "H0000",
				    "responseMessage": "정상처리 되었습니다."
				  },
				  "REC": {
				    "status": "SUCCESS",
				    "transactionUniqueNo": 88,
				    "accountNo": "0011234567890123"
				  }
				}
				""", APPLICATION_JSON));

		AccountVerificationClientDto.CheckVerificationResponse result = client.checkVerification(
			new AccountVerificationClientDto.CheckVerificationRequest(
				"user-key",
				"0011234567890123",
				"AMAGETDONE",
				"1234"
			)
		);

		assertThat(result.status()).isEqualTo("SUCCESS");
		assertThat(result.transactionUniqueNo()).isEqualTo(88L);
		assertThat(result.accountNo()).isEqualTo("0011234567890123");
	}

	@Test
	void checkVerificationMapsMismatchResponseCode() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		AccountVerificationSsafyApiProperties properties = new AccountVerificationSsafyApiProperties();
		properties.setBaseUrl("https://finopenapi.ssafy.io/ssafy/api/v1/edu");
		properties.setInstitutionCode("00100");
		properties.setFintechAppNo("001");
		SsafyAccountVerificationApiClient client = new SsafyAccountVerificationApiClient(
			builder,
			properties,
			new SsafyApiRequestFactory(),
			new SsafyApiResponseValidator()
		);
		ReflectionTestUtils.setField(client, "apiKey", "verification-api-key");

		server.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/accountAuth/checkAuthCode"))
			.andExpect(method(POST))
			.andRespond(withSuccess("""
				{
				  "Header": {
				    "responseCode": "A1088",
				    "responseMessage": "인증코드가 일치하지 않습니다."
				  },
				  "REC": null
				}
				""", APPLICATION_JSON));

		assertThatThrownBy(() -> client.checkVerification(
			new AccountVerificationClientDto.CheckVerificationRequest(
				"user-key",
				"0011234567890123",
				"AMAGETDONE",
				"9999"
			)
		))
			.isInstanceOf(AccountVerificationException.class)
			.extracting("errorCode")
			.isEqualTo(AccountVerificationErrorCode.AUTH_CODE_MISMATCH);
	}
}

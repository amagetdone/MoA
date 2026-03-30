package com.ssafy.amagetdone.domain.member.infra.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ssafy.amagetdone.global.config.SsafyFinApiProperties;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.infra.client.dto.SsafyMemberResponse;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.slf4j.LoggerFactory;

class SsafyFinMemberClientTest {

	@Test
	void createMemberSendsApiKeyAndEmailAndReturnsUserKey() {
		ListAppender<ILoggingEvent> appender = attachAppender();
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		SsafyFinApiProperties properties = new SsafyFinApiProperties();
		properties.setBaseUrl("https://finopenapi.ssafy.io/ssafy/api/v1");
		properties.setApiKey("ssafy-api-key");
		SsafyFinMemberClient client = new SsafyFinMemberClient(builder, properties);

		server.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/member"))
			.andExpect(method(POST))
			.andExpect(content().json("""
				{
				  "apiKey": "ssafy-api-key",
				  "userId": "moa@example.com"
				}
				"""))
			.andRespond(withSuccess("""
				{
				  "userId": "moa@example.com",
				  "userName": "moa",
				  "institutionCode": "00100",
				  "userKey": "cf1d49ba-663b-495d-9227-fc2643aa7c5e",
				  "created": "2024-03-04T12:41:30.921299+09:00",
				  "modified": "2024-03-04T12:41:30.921295+09:00"
				}
				""", APPLICATION_JSON));

		SsafyMemberResponse response = client.createMember("moa@example.com");

		assertEquals("moa@example.com", response.userId());
		assertEquals("cf1d49ba-663b-495d-9227-fc2643aa7c5e", response.userKey());
		assertEquals(true, containsLog(appender, "event=FIN_MEMBER_CREATE_ATTEMPT"));
		assertEquals(true, containsLog(appender, "event=FIN_MEMBER_CREATE_SUCCESS"));
	}

	@Test
	void searchMemberSendsEmailAndApiKeyAndReturnsUserKey() {
		ListAppender<ILoggingEvent> appender = attachAppender();
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		SsafyFinApiProperties properties = new SsafyFinApiProperties();
		properties.setBaseUrl("https://finopenapi.ssafy.io/ssafy/api/v1");
		properties.setApiKey("ssafy-api-key");
		SsafyFinMemberClient client = new SsafyFinMemberClient(builder, properties);

		server.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/member/search"))
			.andExpect(method(POST))
			.andExpect(content().json("""
				{
				  "userId": "moa@example.com",
				  "apiKey": "ssafy-api-key"
				}
				"""))
			.andRespond(withSuccess("""
				{
				  "userId": "moa@example.com",
				  "userName": "moa",
				  "institutionCode": "00100",
				  "userKey": "cf1d49ba-663b-495d-9227-fc2643aa7c5e",
				  "created": "2024-03-04T12:41:30.921299+09:00",
				  "modified": "2024-03-04T12:41:30.921295+09:00"
				}
				""", APPLICATION_JSON));

		SsafyMemberResponse response = client.searchMember("moa@example.com");

		assertEquals("moa@example.com", response.userId());
		assertEquals("cf1d49ba-663b-495d-9227-fc2643aa7c5e", response.userKey());
		assertEquals(true, containsLog(appender, "event=FIN_MEMBER_SEARCH_ATTEMPT"));
		assertEquals(true, containsLog(appender, "event=FIN_MEMBER_SEARCH_SUCCESS"));
	}

	@Test
	void createMemberMapsDuplicateMemberError() {
		ListAppender<ILoggingEvent> appender = attachAppender();
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		SsafyFinApiProperties properties = new SsafyFinApiProperties();
		properties.setBaseUrl("https://finopenapi.ssafy.io/ssafy/api/v1");
		properties.setApiKey("ssafy-api-key");
		SsafyFinMemberClient client = new SsafyFinMemberClient(builder, properties);

		server.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/member"))
			.andExpect(method(POST))
			.andRespond(withStatus(HttpStatus.BAD_REQUEST)
				.contentType(APPLICATION_JSON)
				.body("""
					{
					  "errorCode": "E4002",
					  "message": "이미 존재하는 ID입니다."
					}
					"""));

		MemberException exception = assertThrows(
			MemberException.class,
			() -> client.createMember("moa@example.com")
		);

		assertEquals(MemberErrorCode.FINANCIAL_MEMBER_ALREADY_EXISTS, exception.getErrorCode());
		assertEquals(true, containsLog(appender, "event=FIN_MEMBER_CREATE_FAILURE"));
	}

	private ListAppender<ILoggingEvent> attachAppender() {
		Logger logger = (Logger) LoggerFactory.getLogger(SsafyFinMemberClient.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		return appender;
	}

	private boolean containsLog(ListAppender<ILoggingEvent> appender, String text) {
		return appender.list.stream()
			.anyMatch(event -> event.getFormattedMessage().contains(text));
	}
}

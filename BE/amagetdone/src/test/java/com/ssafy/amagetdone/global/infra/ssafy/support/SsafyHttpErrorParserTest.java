package com.ssafy.amagetdone.global.infra.ssafy.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SsafyHttpErrorParserTest {

	@Test
	void parseExtractsResponseCodeAndMessageFromJsonBody() {
		SsafyHttpErrorParser parser = new SsafyHttpErrorParser();

		SsafyHttpErrorParser.ParsedError result = parser.parse("""
			{
			  "responseCode" : "H1008",
			  "responseMessage" : "API_KEY가 유효하지 않습니다."
			}
			""");

		assertThat(result.responseCode()).isEqualTo("H1008");
		assertThat(result.responseMessage()).isEqualTo("API_KEY가 유효하지 않습니다.");
	}

	@Test
	void parseReturnsNullValuesForInvalidBody() {
		SsafyHttpErrorParser parser = new SsafyHttpErrorParser();

		SsafyHttpErrorParser.ParsedError result = parser.parse("not-json");

		assertThat(result.responseCode()).isNull();
		assertThat(result.responseMessage()).isNull();
	}
}

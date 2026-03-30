package com.ssafy.amagetdone.domain.member.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public final class TestSocialAuthDto {

	private TestSocialAuthDto() {
	}

	@Schema(name = "TestSocialAuthRequest")
	public record Request(
		@Schema(description = "테스트 사용자 번호", defaultValue = "1", allowableValues = {"1", "2", "3", "4", "5"})
		@Min(1)
		@Max(5)
		Integer id
	) {
		public Request {
			id = id == null ? 1 : id;
		}

		public static Request defaultRequest() {
			return new Request(1);
		}
	}
}

package com.ssafy.amagetdone.domain.asset.infra.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public final class MerchantDto {

	private MerchantDto() {
	}

	public record Request() {
	}

	public record Record(
		String categoryId,
		String categoryName,
		Long merchantId,
		String merchantName
	) {
	}

	public static final class Response {

		private final List<Record> records;

		@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
		public Response(List<Record> records) {
			this.records = records;
		}

		@JsonValue
		public List<Record> records() {
			return records;
		}
	}
}

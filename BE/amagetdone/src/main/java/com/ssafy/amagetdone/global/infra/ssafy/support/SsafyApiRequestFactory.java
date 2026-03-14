package com.ssafy.amagetdone.global.infra.ssafy.support;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import com.ssafy.amagetdone.global.infra.ssafy.envelope.SsafyRequestEnvelope;
import com.ssafy.amagetdone.global.infra.ssafy.exception.SsafyApiException;
import com.ssafy.amagetdone.global.infra.ssafy.model.SsafyRequest;
import com.ssafy.amagetdone.global.infra.ssafy.model.SsafyRequestHeader;

import org.springframework.stereotype.Component;

@Component
public class SsafyApiRequestFactory {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");
	private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
	private final Clock clock;

	public SsafyApiRequestFactory() {
		this(Clock.system(KST_ZONE));
	}

	public SsafyApiRequestFactory(Clock clock) {
		this.clock = clock;
	}

	public <T> SsafyRequestEnvelope<T> create(
		String apiName,
		String institutionCode,
		String fintechAppNo,
		String apiKey,
		String userKey,
		T payload
	) {
		if (isBlank(apiName) || isBlank(institutionCode) || isBlank(fintechAppNo) || isBlank(apiKey)) {
			throw new SsafyApiException(SsafyApiException.Reason.INVALID_CONFIGURATION);
		}

		LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), KST_ZONE);
		SsafyRequestHeader header = new SsafyRequestHeader(
			apiName,
			now.format(DATE_FORMAT),
			now.format(TIME_FORMAT),
			institutionCode,
			fintechAppNo,
			apiName,
			createInstitutionTransactionUniqueNo(now),
			apiKey,
			blankToNull(userKey)
		);

		return SsafyRequestEnvelope.from(new SsafyRequest<>(header, payload));
	}

	private String createInstitutionTransactionUniqueNo(LocalDateTime now) {
		String prefix = now.format(DATE_FORMAT) + now.format(TIME_FORMAT);
		int suffix = ThreadLocalRandom.current().nextInt(0, 1_000_000);
		return prefix + "%06d".formatted(suffix);
	}

	private String blankToNull(String value) {
		return isBlank(value) ? null : value;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}

package com.ssafy.amagetdone.domain.plan.infra.client;

import com.ssafy.amagetdone.domain.plan.infra.client.dto.PyanGenerateResponse;

public interface PyanClient {

	PyanGenerateResponse generate(byte[] imageBytes, String goalId, String requestKey);

	PyanGenerateResponse generateDemo(
		byte[] imageBytes,
		String goalId,
		String requestKey,
		Integer userId,
		Integer planId,
		String planTitle
	);

	PyanGenerateResponse fetchStatus(String statusUrl);
}

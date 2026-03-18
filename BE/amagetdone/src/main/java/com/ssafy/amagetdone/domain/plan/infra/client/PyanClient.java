package com.ssafy.amagetdone.domain.plan.infra.client;

import com.ssafy.amagetdone.domain.plan.infra.client.dto.PyanGenerateResponse;

public interface PyanClient {

	PyanGenerateResponse generate(byte[] imageBytes, String goalId);
}

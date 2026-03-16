package com.ssafy.amagetdone.global.docs.controller;

import com.ssafy.amagetdone.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Global", description = "Global operational APIs")
public interface GlobalHealthCheckDocsController {

	@Operation(summary = "Health check", description = "Returns a simple health response.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Health check succeeded")
	})
	ApiResponse<String> health();
}

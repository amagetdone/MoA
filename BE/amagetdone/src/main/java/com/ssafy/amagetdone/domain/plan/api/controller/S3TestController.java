package com.ssafy.amagetdone.domain.plan.api.controller;

import com.ssafy.amagetdone.global.infra.storage.ImageCategory;
import com.ssafy.amagetdone.global.infra.storage.S3StorageService;
import com.ssafy.amagetdone.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "S3 Test", description = "Local manual test endpoints for S3 upload and delete flows")
@RestController
@RequestMapping("/test/s3")
public class S3TestController {

	private final S3StorageService s3StorageService;

	public S3TestController(S3StorageService s3StorageService) {
		this.s3StorageService = s3StorageService;
	}

	@Operation(
		summary = "Upload a file to S3",
		description = "Uploads a multipart file to the profile prefix in S3 and returns the stored object key."
	)
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<String> upload(
		@Parameter(
			description = "File to upload",
			content = @Content(
				mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
				schema = @Schema(type = "string", format = "binary")
			)
		)
		@RequestParam("file") MultipartFile file
	) {
		String objectKey = s3StorageService.upload(file, ImageCategory.PROFILE);
		return ApiResponse.ok("S3 upload test succeeded.", objectKey);
	}

	@Operation(
		summary = "Delete an uploaded S3 file",
		description = "Deletes an S3 object by using the object key returned from the upload test API."
	)
	@DeleteMapping
	public ApiResponse<Boolean> delete(
		@Parameter(
			description = "S3 object key returned by the upload API",
			example = "profile/test.png"
		)
		@RequestParam("objectKey") String objectKey
	) {
		s3StorageService.delete(objectKey);
		return ApiResponse.ok("S3 delete test succeeded.", true);
	}
}

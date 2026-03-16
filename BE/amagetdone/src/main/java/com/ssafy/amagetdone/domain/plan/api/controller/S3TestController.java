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

@Tag(name = "S3 테스트", description = "S3 업로드와 삭제 흐름을 수동으로 확인하는 로컬 테스트 API")
@RestController
@RequestMapping("/test/s3")
public class S3TestController {

	private final S3StorageService s3StorageService;

	public S3TestController(S3StorageService s3StorageService) {
		this.s3StorageService = s3StorageService;
	}

	@Operation(
		summary = "S3 파일 업로드 테스트",
		description = "멀티파트 파일을 S3의 프로필 경로에 업로드하고 저장된 객체 키를 반환합니다."
	)
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<String> upload(
		@Parameter(
			description = "업로드할 파일",
			content = @Content(
				mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
				schema = @Schema(type = "string", format = "binary")
			)
		)
		@RequestParam("file") MultipartFile file
	) {
		String objectKey = s3StorageService.upload(file, ImageCategory.PROFILE);
		return ApiResponse.ok("S3 업로드 테스트에 성공했습니다.", objectKey);
	}

	@Operation(
		summary = "S3 업로드 파일 삭제 테스트",
		description = "업로드 테스트 API가 반환한 객체 키로 S3 객체를 삭제합니다."
	)
	@DeleteMapping
	public ApiResponse<Boolean> delete(
		@Parameter(
			description = "업로드 API가 반환한 S3 객체 키",
			example = "profile/test.png"
		)
		@RequestParam("objectKey") String objectKey
	) {
		s3StorageService.delete(objectKey);
		return ApiResponse.ok("S3 삭제 테스트에 성공했습니다.", true);
	}
}

package com.ssafy.amagetdone.domain.plan.api.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.amagetdone.global.config.SecurityConfig;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.global.infra.storage.ImageCategory;
import com.ssafy.amagetdone.global.infra.storage.S3StorageService;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(S3TestController.class)
@Import(SecurityConfig.class)
class S3TestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private S3StorageService s3StorageService;

	@MockitoBean
	private JwtAccessTokenReader jwtAccessTokenReader;

	@Test
	void uploadReturnsUploadedUrl() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"profile.png",
			"image/png",
			"image-content".getBytes()
		);
		given(s3StorageService.upload(file, ImageCategory.PROFILE)).willReturn("profile/test.png");

		mockMvc.perform(multipart("/api/test/s3/upload")
				.file(file)
				.contentType(MULTIPART_FORM_DATA)
				.with(csrf())
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				))
				.with(apiContextPath()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("S3 upload test succeeded."))
			.andExpect(jsonPath("$.data").value("profile/test.png"));
	}

	@Test
	void deleteRemovesObjectAndReturnsSuccess() throws Exception {
		String objectKey = "profile/test.png";

		mockMvc.perform(delete("/api/test/s3")
				.with(apiContextPath())
				.with(csrf())
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				))
				.param("objectKey", objectKey))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("S3 delete test succeeded."))
			.andExpect(jsonPath("$.data").value(true));

		verify(s3StorageService).delete(objectKey);
	}

	private RequestPostProcessor apiContextPath() {
		return request -> {
			request.setContextPath("/api");
			return request;
		};
	}
}

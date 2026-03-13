package com.ssafy.amagetdone.domain.plan.api.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.amagetdone.domain.plan.infra.storage.S3Uploader;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(S3TestController.class)
class S3TestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private S3Uploader s3Uploader;

	@Test
	void uploadReturnsUploadedUrl() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"profile.png",
			"image/png",
			"image-content".getBytes()
		);
		given(s3Uploader.upload(file, "test")).willReturn("https://bucket.s3.ap-northeast-2.amazonaws.com/test/profile.png");

		mockMvc.perform(multipart("/api/test/s3/upload")
				.file(file)
				.contentType(MULTIPART_FORM_DATA)
				.with(apiContextPath()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("S3 upload test succeeded."))
			.andExpect(jsonPath("$.data").value("https://bucket.s3.ap-northeast-2.amazonaws.com/test/profile.png"));
	}

	@Test
	void deleteRemovesObjectAndReturnsSuccess() throws Exception {
		String fileUrl = "https://bucket.s3.ap-northeast-2.amazonaws.com/test/profile.png";

		mockMvc.perform(delete("/api/test/s3")
				.with(apiContextPath())
				.param("fileUrl", fileUrl))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("S3 delete test succeeded."))
			.andExpect(jsonPath("$.data").value(true));

		verify(s3Uploader).delete(fileUrl);
	}

	private RequestPostProcessor apiContextPath() {
		return request -> {
			request.setContextPath("/api");
			return request;
		};
	}
}

package com.ssafy.amagetdone.domain.plan.infra.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class S3UploaderTest {

	@Mock
	private AmazonS3 amazonS3;

	private S3Uploader s3Uploader;

	@BeforeEach
	void setUp() {
		s3Uploader = new S3Uploader(amazonS3);
		ReflectionTestUtils.setField(s3Uploader, "bucket", "test-bucket");
	}

	@Test
	void uploadsMultipartFileAndReturnsUrl() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
			"image",
			"profile.png",
			"image/png",
			"image-content".getBytes(StandardCharsets.UTF_8)
		);

		when(amazonS3.getUrl(eq("test-bucket"), any(String.class)))
			.thenReturn(java.net.URI.create("https://test-bucket.s3.ap-northeast-2.amazonaws.com/profile/test.png").toURL());

		String uploadedUrl = s3Uploader.upload(file, "profile");

		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<ObjectMetadata> metadataCaptor = ArgumentCaptor.forClass(ObjectMetadata.class);

		verify(amazonS3).putObject(eq("test-bucket"), keyCaptor.capture(), any(), metadataCaptor.capture());
		assertEquals("image/png", metadataCaptor.getValue().getContentType());
		assertEquals(file.getSize(), metadataCaptor.getValue().getContentLength());
		assertEquals("https://test-bucket.s3.ap-northeast-2.amazonaws.com/profile/test.png", uploadedUrl);
		org.junit.jupiter.api.Assertions.assertTrue(keyCaptor.getValue().startsWith("profile/"));
		org.junit.jupiter.api.Assertions.assertTrue(keyCaptor.getValue().endsWith(".png"));
	}

	@Test
	void deletesObjectUsingUrlPath() {
		s3Uploader.delete("https://test-bucket.s3.ap-northeast-2.amazonaws.com/profile/sample.png");

		verify(amazonS3).deleteObject("test-bucket", "profile/sample.png");
	}

	@Test
	void rejectsEmptyFile() {
		MockMultipartFile emptyFile = new MockMultipartFile("image", "", "image/png", new byte[0]);

		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> s3Uploader.upload(emptyFile, "profile")
		);

		assertEquals("File is empty.", exception.getMessage());
	}
}

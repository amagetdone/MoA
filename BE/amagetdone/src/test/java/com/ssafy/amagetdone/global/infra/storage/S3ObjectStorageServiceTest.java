package com.ssafy.amagetdone.global.infra.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class S3ObjectStorageServiceTest {

	@Mock
	private AmazonS3 amazonS3;

	private S3ObjectStorageService storageService;

	@BeforeEach
	void setUp() {
		storageService = new S3ObjectStorageService(amazonS3);
		ReflectionTestUtils.setField(storageService, "bucket", "test-bucket");
		ReflectionTestUtils.setField(storageService, "region", "ap-northeast-2");
	}

	@Test
	void uploadStoresFileUnderProfilePrefix() {
		MockMultipartFile file = new MockMultipartFile(
			"image",
			"profile.png",
			"image/png",
			"image-content".getBytes(StandardCharsets.UTF_8)
		);

		String objectKey = storageService.upload(file, ImageCategory.PROFILE);

		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<ObjectMetadata> metadataCaptor = ArgumentCaptor.forClass(ObjectMetadata.class);
		verify(amazonS3).putObject(eq("test-bucket"), keyCaptor.capture(), any(), metadataCaptor.capture());
		assertEquals("image/png", metadataCaptor.getValue().getContentType());
		assertEquals(file.getSize(), metadataCaptor.getValue().getContentLength());
		org.junit.jupiter.api.Assertions.assertTrue(objectKey.startsWith("profile/"));
		org.junit.jupiter.api.Assertions.assertTrue(objectKey.endsWith(".png"));
	}

	@Test
	void uploadFallsBackToContentTypeWhenFileNameExtensionIsMissing() {
		MockMultipartFile file = new MockMultipartFile(
			"image",
			"profile-image",
			"image/png",
			"image-content".getBytes(StandardCharsets.UTF_8)
		);

		String objectKey = storageService.upload(file, ImageCategory.PROFILE);

		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		verify(amazonS3).putObject(eq("test-bucket"), keyCaptor.capture(), any(), any(ObjectMetadata.class));
		org.junit.jupiter.api.Assertions.assertTrue(objectKey.startsWith("profile/"));
		org.junit.jupiter.api.Assertions.assertTrue(objectKey.endsWith(".png"));
	}

	@Test
	void loadBytesReadsObjectContent() throws Exception {
		S3Object s3Object = new S3Object();
		s3Object.setObjectContent(new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)));
		when(amazonS3.getObject("test-bucket", "profile/sample.png")).thenReturn(s3Object);

		byte[] bytes = storageService.loadBytes("profile/sample.png");

		assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), bytes);
	}

	@Test
	void deleteRemovesObjectByKey() {
		storageService.delete("plan/sample.png");

		verify(amazonS3).deleteObject("test-bucket", "plan/sample.png");
	}

	@Test
	void getUrlBuildsStableRegionalVirtualHostedStyleUrl() {
		String url = storageService.getUrl("profile/sample image.png");

		assertEquals(
			"https://test-bucket.s3.ap-northeast-2.amazonaws.com/profile/sample%20image.png",
			url
		);
		verify(amazonS3, never()).getUrl(any(), any());
	}

	@Test
	void uploadRejectsEmptyFile() {
		MockMultipartFile emptyFile = new MockMultipartFile("image", "", "image/png", new byte[0]);

		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> storageService.upload(emptyFile, ImageCategory.PROFILE)
		);

		assertEquals("File is empty.", exception.getMessage());
	}

	@Test
	void uploadRejectsFileWithoutExtensionAndContentType() {
		MockMultipartFile file = new MockMultipartFile("image", "profile-image", null, "abc".getBytes(StandardCharsets.UTF_8));

		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> storageService.upload(file, ImageCategory.PROFILE)
		);

		assertEquals("File extension is missing.", exception.getMessage());
	}
}

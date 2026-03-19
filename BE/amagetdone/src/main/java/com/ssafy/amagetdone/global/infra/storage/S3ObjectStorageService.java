package com.ssafy.amagetdone.global.infra.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class S3ObjectStorageService implements S3StorageService {

	private final AmazonS3 amazonS3;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	@Value("${cloud.aws.region.static-region}")
	private String region;

	public S3ObjectStorageService(AmazonS3 amazonS3) {
		this.amazonS3 = amazonS3;
	}

	@Override
	public String upload(MultipartFile file, ImageCategory category) {
		validateFile(file);

		String objectKey = category.getPrefix() + "/" + UUID.randomUUID() + "." + extractExt(file.getOriginalFilename());
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(file.getSize());
		metadata.setContentType(file.getContentType());

		try {
			amazonS3.putObject(bucket, objectKey, file.getInputStream(), metadata);
		} catch (IOException exception) {
			throw new IllegalStateException("S3 upload failed.", exception);
		}

		return objectKey;
	}

	@Override
	public String getUrl(String objectKey) {
		String encodedKey = UriUtils.encodePath(objectKey, StandardCharsets.UTF_8);
		return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + encodedKey;
	}

	@Override
	public byte[] loadBytes(String objectKey) {
		try (var inputStream = amazonS3.getObject(bucket, objectKey).getObjectContent()) {
			return inputStream.readAllBytes();
		} catch (IOException exception) {
			throw new IllegalStateException("S3 read failed.", exception);
		}
	}

	@Override
	public void delete(String objectKey) {
		amazonS3.deleteObject(bucket, objectKey);
	}

	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("File is empty.");
		}
	}

	private String extractExt(String fileName) {
		if (fileName == null) {
			throw new IllegalArgumentException("File extension is missing.");
		}
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
			throw new IllegalArgumentException("File extension is missing.");
		}
		return fileName.substring(dotIndex + 1);
	}
}

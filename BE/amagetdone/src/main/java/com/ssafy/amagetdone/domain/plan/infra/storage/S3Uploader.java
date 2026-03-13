package com.ssafy.amagetdone.domain.plan.infra.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class S3Uploader {

	private final AmazonS3 amazonS3;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	public S3Uploader(AmazonS3 amazonS3) {
		this.amazonS3 = amazonS3;
	}

	public String upload(MultipartFile file, String dir) {
		validateFile(file);

		String fileName = createFileName(dir, file.getOriginalFilename());
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(file.getSize());
		metadata.setContentType(file.getContentType());

		try {
			amazonS3.putObject(bucket, fileName, file.getInputStream(), metadata);
		} catch (IOException exception) {
			throw new RuntimeException("S3 upload failed.", exception);
		}

		return amazonS3.getUrl(bucket, fileName).toString();
	}

	public String upload(byte[] fileBytes, String originalName, String contentType, String dir) {
		validateBytes(fileBytes);

		String fileName = createFileName(dir, originalName);
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(fileBytes.length);
		metadata.setContentType(contentType);

		amazonS3.putObject(bucket, fileName, new ByteArrayInputStream(fileBytes), metadata);
		return amazonS3.getUrl(bucket, fileName).toString();
	}

	public void delete(String fileUrl) {
		String key = extractKey(fileUrl);
		amazonS3.deleteObject(bucket, key);
	}

	private void validateFile(MultipartFile file) {
		if (file.isEmpty()) {
			throw new IllegalArgumentException("File is empty.");
		}
	}

	private void validateBytes(byte[] fileBytes) {
		if (fileBytes == null || fileBytes.length == 0) {
			throw new IllegalArgumentException("File is empty.");
		}
	}

	private String createFileName(String dir, String originalName) {
		String ext = extractExt(originalName);
		return dir + "/" + UUID.randomUUID() + "." + ext;
	}

	private String extractExt(String fileName) {
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
			throw new IllegalArgumentException("File extension is missing.");
		}
		return fileName.substring(dotIndex + 1);
	}

	private String extractKey(String fileUrl) {
		return fileUrl.substring(fileUrl.indexOf(".com/") + 5);
	}
}

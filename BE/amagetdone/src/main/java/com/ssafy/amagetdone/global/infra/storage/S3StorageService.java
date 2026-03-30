package com.ssafy.amagetdone.global.infra.storage;

import org.springframework.web.multipart.MultipartFile;

public interface S3StorageService {

	String upload(MultipartFile file, ImageCategory category);

	String getUrl(String objectKey);

	byte[] loadBytes(String objectKey);

	void delete(String objectKey);
}

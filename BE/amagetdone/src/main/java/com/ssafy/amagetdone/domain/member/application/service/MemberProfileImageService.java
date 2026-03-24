package com.ssafy.amagetdone.domain.member.application.service;

import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.global.infra.storage.ImageCategory;
import com.ssafy.amagetdone.global.infra.storage.S3StorageService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MemberProfileImageService {

	private static final String DEFAULT_PROFILE_IMAGE_URL =
		"https://amagetdone.s3.ap-northeast-2.amazonaws.com/profile/fb9dc026-62fc-4c52-87de-d277cecb6e57.png";

	private final UserRepository userRepository;
	private final S3StorageService storageService;

	public MemberProfileImageService(UserRepository userRepository, S3StorageService storageService) {
		this.userRepository = userRepository;
		this.storageService = storageService;
	}

	@Transactional
	public String replaceProfileImage(Integer userId, MultipartFile file) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new MemberException(MemberErrorCode.USER_NOT_FOUND));

		if (hasText(user.getProfileUrl())) {
			storageService.delete(user.getProfileUrl());
		}

		String objectKey = storageService.upload(file, ImageCategory.PROFILE);
		user.updateProfileUrl(objectKey);
		return objectKey;
	}

	public byte[] loadProfileImageBytes(Integer userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new MemberException(MemberErrorCode.USER_NOT_FOUND));
		if (!hasText(user.getProfileUrl())) {
			throw new IllegalArgumentException("Profile image is missing.");
		}
		return storageService.loadBytes(user.getProfileUrl());
	}

	public String loadProfileImageUrl(Integer userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new MemberException(MemberErrorCode.USER_NOT_FOUND));
		if (!hasText(user.getProfileUrl())) {
			return DEFAULT_PROFILE_IMAGE_URL;
		}
		return storageService.getUrl(user.getProfileUrl());
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}

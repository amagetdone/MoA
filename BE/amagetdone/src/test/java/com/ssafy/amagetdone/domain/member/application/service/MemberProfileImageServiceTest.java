package com.ssafy.amagetdone.domain.member.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.global.infra.storage.ImageCategory;
import com.ssafy.amagetdone.global.infra.storage.S3StorageService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberProfileImageServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private S3StorageService storageService;

	@InjectMocks
	private MemberProfileImageService memberProfileImageService;

	@Test
	void replaceProfileImageDeletesOldKeyAndStoresNewKey() {
		User user = newUser(1, "profile/old.png");
		MockMultipartFile file = new MockMultipartFile("image", "new.png", "image/png", "abc".getBytes());
		given(userRepository.findById(1)).willReturn(Optional.of(user));
		given(storageService.upload(file, ImageCategory.PROFILE)).willReturn("profile/new.png");

		String objectKey = memberProfileImageService.replaceProfileImage(1, file);

		verify(storageService).delete("profile/old.png");
		verify(storageService).upload(file, ImageCategory.PROFILE);
		assertEquals("profile/new.png", objectKey);
		assertEquals("profile/new.png", user.getProfileUrl());
	}

	@Test
	void replaceProfileImageSkipsDeleteWhenOldKeyMissing() {
		User user = newUser(1, null);
		MockMultipartFile file = new MockMultipartFile("image", "new.png", "image/png", "abc".getBytes());
		given(userRepository.findById(1)).willReturn(Optional.of(user));
		given(storageService.upload(file, ImageCategory.PROFILE)).willReturn("profile/new.png");

		memberProfileImageService.replaceProfileImage(1, file);

		verify(storageService, never()).delete(any());
	}

	@Test
	void replaceProfileImageThrowsWhenUserMissing() {
		MockMultipartFile file = new MockMultipartFile("image", "new.png", "image/png", "abc".getBytes());
		given(userRepository.findById(1)).willReturn(Optional.empty());

		MemberException exception = assertThrows(
			MemberException.class,
			() -> memberProfileImageService.replaceProfileImage(1, file)
		);

		assertEquals(MemberErrorCode.USER_NOT_FOUND, exception.getErrorCode());
	}

	@Test
	void loadProfileImageBytesReadsStoredKey() {
		User user = newUser(1, "profile/current.png");
		given(userRepository.findById(1)).willReturn(Optional.of(user));
		given(storageService.loadBytes("profile/current.png")).willReturn("abc".getBytes());

		byte[] bytes = memberProfileImageService.loadProfileImageBytes(1);

		assertEquals("abc", new String(bytes));
		verify(storageService).loadBytes("profile/current.png");
	}

	@Test
	void loadProfileImageUrlResolvesStoredKeyToS3Url() {
		User user = newUser(1, "profile/current.png");
		given(userRepository.findById(1)).willReturn(Optional.of(user));
		given(storageService.getUrl("profile/current.png")).willReturn("https://bucket.s3.ap-northeast-2.amazonaws.com/profile/current.png");

		String url = memberProfileImageService.loadProfileImageUrl(1);

		assertEquals("https://bucket.s3.ap-northeast-2.amazonaws.com/profile/current.png", url);
		verify(storageService).getUrl("profile/current.png");
	}

	@Test
	void loadProfileImageUrlReturnsDefaultUrlWhenStoredKeyMissing() {
		User user = newUser(1, null);
		given(userRepository.findById(1)).willReturn(Optional.of(user));

		String url = memberProfileImageService.loadProfileImageUrl(1);

		assertEquals(
			"https://amagetdone.s3.ap-northeast-2.amazonaws.com/profile/fb9dc026-62fc-4c52-87de-d277cecb6e57.png",
			url
		);
		verify(storageService, never()).getUrl(any());
	}

	private User newUser(int id, String profileUrl) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", id);
		ReflectionTestUtils.setField(user, "profileUrl", profileUrl);
		return user;
	}
}

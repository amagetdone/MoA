package com.ssafy.amagetdone.domain.member.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.List;

import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.infra.client.SsafyFinMemberClient;
import com.ssafy.amagetdone.domain.member.infra.client.dto.SsafyMemberResponse;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnsureFinMemberLinkedUseCaseTest {

	@Mock
	private SsafyFinMemberClient ssafyFinMemberClient;

	@Mock
	private UserRepository userRepository;

	@Test
	void executeReturnsUserKeyWhenCreateSucceeds() {
		EnsureFinMemberLinkedUseCase useCase = new EnsureFinMemberLinkedUseCase(ssafyFinMemberClient, userRepository);
		given(ssafyFinMemberClient.createMember("moa@example.com"))
			.willReturn(new SsafyMemberResponse(
				"moa@example.com",
				"moa",
				"00100",
				"user-key",
				"2024-03-04T12:41:30.921299+09:00",
				"2024-03-04T12:41:30.921295+09:00"
			));
		given(userRepository.findAllByUserKey("user-key")).willReturn(List.of());

		String userKey = useCase.execute(7, "moa@example.com");

		assertEquals("user-key", userKey);
		verify(ssafyFinMemberClient).createMember("moa@example.com");
	}

	@Test
	void executeFallsBackToSearchWhenCreateReportsDuplicateAndUserKeyIsUnclaimed() {
		EnsureFinMemberLinkedUseCase useCase = new EnsureFinMemberLinkedUseCase(ssafyFinMemberClient, userRepository);
		given(ssafyFinMemberClient.createMember("moa@example.com"))
			.willThrow(new MemberException(MemberErrorCode.FINANCIAL_MEMBER_ALREADY_EXISTS));
		given(ssafyFinMemberClient.searchMember("moa@example.com"))
			.willReturn(new SsafyMemberResponse(
				"moa@example.com",
				"moa",
				"00100",
				"user-key",
				"2024-03-04T12:41:30.921299+09:00",
				"2024-03-04T12:41:30.921295+09:00"
			));
		given(userRepository.findAllByUserKey("user-key")).willReturn(List.of());

		String userKey = useCase.execute(7, "moa@example.com");

		assertEquals("user-key", userKey);
		verify(ssafyFinMemberClient).searchMember("moa@example.com");
	}

	@Test
	void executeThrowsWhenCreateAndSearchBothFail() {
		EnsureFinMemberLinkedUseCase useCase = new EnsureFinMemberLinkedUseCase(ssafyFinMemberClient, userRepository);
		given(ssafyFinMemberClient.createMember("moa@example.com"))
			.willThrow(new MemberException(MemberErrorCode.FINANCIAL_MEMBER_ALREADY_EXISTS));
		given(ssafyFinMemberClient.searchMember("moa@example.com"))
			.willThrow(new MemberException(MemberErrorCode.FINANCIAL_MEMBER_NOT_FOUND));

		MemberException exception = assertThrows(
			MemberException.class,
			() -> useCase.execute(7, "moa@example.com")
		);

		assertEquals(MemberErrorCode.FINANCIAL_MEMBER_NOT_FOUND, exception.getErrorCode());
	}

	@Test
	void executeThrowsWhenResolvedUserKeyBelongsToAnotherLocalUser() {
		EnsureFinMemberLinkedUseCase useCase = new EnsureFinMemberLinkedUseCase(ssafyFinMemberClient, userRepository);
		given(ssafyFinMemberClient.createMember("moa@example.com"))
			.willThrow(new MemberException(MemberErrorCode.FINANCIAL_MEMBER_ALREADY_EXISTS));
		given(ssafyFinMemberClient.searchMember("moa@example.com"))
			.willReturn(new SsafyMemberResponse(
				"moa@example.com",
				"moa",
				"00100",
				"user-key",
				"2024-03-04T12:41:30.921299+09:00",
				"2024-03-04T12:41:30.921295+09:00"
			));
		User existingOwner = User.createPendingSocialUser();
		setUserId(existingOwner, 99);
		given(userRepository.findAllByUserKey("user-key")).willReturn(List.of(existingOwner));

		MemberException exception = assertThrows(
			MemberException.class,
			() -> useCase.execute(7, "moa@example.com")
		);

		assertEquals(MemberErrorCode.FINANCIAL_MEMBER_ALREADY_LINKED, exception.getErrorCode());
	}

	@Test
	void executeAllowsCurrentUserWhenDuplicateOwnersIncludeRequester() {
		EnsureFinMemberLinkedUseCase useCase = new EnsureFinMemberLinkedUseCase(ssafyFinMemberClient, userRepository);
		given(ssafyFinMemberClient.createMember("moa@example.com"))
			.willThrow(new MemberException(MemberErrorCode.FINANCIAL_MEMBER_ALREADY_EXISTS));
		given(ssafyFinMemberClient.searchMember("moa@example.com"))
			.willReturn(new SsafyMemberResponse(
				"moa@example.com",
				"moa",
				"00100",
				"user-key",
				"2024-03-04T12:41:30.921299+09:00",
				"2024-03-04T12:41:30.921295+09:00"
			));
		User currentOwner = User.createPendingSocialUser();
		setUserId(currentOwner, 7);
		User duplicateOwner = User.createPendingSocialUser();
		setUserId(duplicateOwner, 99);
		given(userRepository.findAllByUserKey("user-key")).willReturn(List.of(currentOwner, duplicateOwner));

		String userKey = useCase.execute(7, "moa@example.com");

		assertEquals("user-key", userKey);
		verify(ssafyFinMemberClient).searchMember("moa@example.com");
	}

	private void setUserId(User user, int id) {
		try {
			Field idField = User.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(user, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}

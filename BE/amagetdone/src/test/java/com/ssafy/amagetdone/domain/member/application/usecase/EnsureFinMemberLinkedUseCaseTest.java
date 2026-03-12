package com.ssafy.amagetdone.domain.member.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.infra.client.SsafyFinMemberClient;
import com.ssafy.amagetdone.domain.member.infra.client.dto.SsafyMemberResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnsureFinMemberLinkedUseCaseTest {

	@Mock
	private SsafyFinMemberClient ssafyFinMemberClient;

	@Test
	void executeReturnsUserKeyWhenCreateSucceeds() {
		EnsureFinMemberLinkedUseCase useCase = new EnsureFinMemberLinkedUseCase(ssafyFinMemberClient);
		given(ssafyFinMemberClient.createMember("moa@example.com"))
			.willReturn(new SsafyMemberResponse(
				"moa@example.com",
				"moa",
				"00100",
				"user-key",
				"2024-03-04T12:41:30.921299+09:00",
				"2024-03-04T12:41:30.921295+09:00"
			));

		String userKey = useCase.execute("moa@example.com");

		assertEquals("user-key", userKey);
		verify(ssafyFinMemberClient).createMember("moa@example.com");
	}

	@Test
	void executeFallsBackToSearchWhenCreateReportsDuplicate() {
		EnsureFinMemberLinkedUseCase useCase = new EnsureFinMemberLinkedUseCase(ssafyFinMemberClient);
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

		String userKey = useCase.execute("moa@example.com");

		assertEquals("user-key", userKey);
		verify(ssafyFinMemberClient).searchMember("moa@example.com");
	}

	@Test
	void executeThrowsWhenCreateAndSearchBothFail() {
		EnsureFinMemberLinkedUseCase useCase = new EnsureFinMemberLinkedUseCase(ssafyFinMemberClient);
		given(ssafyFinMemberClient.createMember("moa@example.com"))
			.willThrow(new MemberException(MemberErrorCode.FINANCIAL_MEMBER_ALREADY_EXISTS));
		given(ssafyFinMemberClient.searchMember("moa@example.com"))
			.willThrow(new MemberException(MemberErrorCode.FINANCIAL_MEMBER_NOT_FOUND));

		MemberException exception = assertThrows(
			MemberException.class,
			() -> useCase.execute("moa@example.com")
		);

		assertEquals(MemberErrorCode.FINANCIAL_MEMBER_NOT_FOUND, exception.getErrorCode());
	}
}

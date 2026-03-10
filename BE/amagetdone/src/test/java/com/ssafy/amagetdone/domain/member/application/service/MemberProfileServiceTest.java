package com.ssafy.amagetdone.domain.member.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;

import com.ssafy.amagetdone.domain.member.api.dto.ProfileCompletionDto;
import com.ssafy.amagetdone.domain.member.application.usecase.CompleteMemberProfileUseCase;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
@ExtendWith(MockitoExtension.class)
class MemberProfileServiceTest {

	@Mock
	private UserRepository userRepository;

	@Test
	void completeProfileActivatesPendingMember() {
		CompleteMemberProfileUseCase completeMemberProfileUseCase = new CompleteMemberProfileUseCase(userRepository);
		MemberProfileService memberProfileService = new MemberProfileService(completeMemberProfileUseCase);
		User user = User.createPendingSocialUser("USER-001");
		setUserId(user, 1);
		given(userRepository.findById(1)).willReturn(Optional.of(user));

		ProfileCompletionDto.Response response = memberProfileService.completeProfile(
			1,
			new ProfileCompletionDto.Request(
				"moa",
				"moa@example.com",
				LocalDate.of(1997, 3, 1),
				true
			)
		);

		assertEquals(1, response.userId());
		assertTrue(response.profileCompleted());
		assertEquals("ACTIVE", response.status());
		assertEquals("GO_HOME", response.nextAction());
	}

	@Test
	void completeProfileThrowsNotFoundWhenMemberDoesNotExist() {
		CompleteMemberProfileUseCase completeMemberProfileUseCase = new CompleteMemberProfileUseCase(userRepository);
		MemberProfileService memberProfileService = new MemberProfileService(completeMemberProfileUseCase);
		given(userRepository.findById(99)).willReturn(Optional.empty());

		MemberException exception = assertThrows(
			MemberException.class,
			() -> memberProfileService.completeProfile(
				99,
				new ProfileCompletionDto.Request(
					"moa",
					"moa@example.com",
					LocalDate.of(1997, 3, 1),
					true
				)
			)
		);
		assertEquals(MemberErrorCode.USER_NOT_FOUND, exception.getErrorCode());
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

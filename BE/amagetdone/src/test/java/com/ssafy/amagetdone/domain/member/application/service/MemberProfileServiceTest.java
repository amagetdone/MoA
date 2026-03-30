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
import com.ssafy.amagetdone.domain.member.application.usecase.EnsureFinMemberLinkedUseCase;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
@ExtendWith(MockitoExtension.class)
class MemberProfileServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private EnsureFinMemberLinkedUseCase ensureFinMemberLinkedUseCase;

	@Test
	void completeProfileActivatesPendingMember() {
		ListAppender<ILoggingEvent> appender = attachAppender();
		CompleteMemberProfileUseCase completeMemberProfileUseCase = new CompleteMemberProfileUseCase(
			userRepository,
			ensureFinMemberLinkedUseCase
		);
		MemberProfileService memberProfileService = new MemberProfileService(completeMemberProfileUseCase);
		User user = User.createPendingSocialUser();
		setUserId(user, 1);
		given(userRepository.findById(1)).willReturn(Optional.of(user));
		given(ensureFinMemberLinkedUseCase.execute(1, "moa@example.com")).willReturn("ssafy-user-key");

		ProfileCompletionDto.Response response = memberProfileService.completeProfile(
			1,
			new ProfileCompletionDto.Request(
				"moa",
				"moa@example.com",
				LocalDate.of(1997, 3, 1),
				true
			)
		);

		assertEquals("ACTIVE", response.status());
		assertEquals("ssafy-user-key", user.getUserKey());
		assertTrue(containsLog(appender, "event=MEMBER_SIGNUP_COMPLETE_SUCCESS"));
	}

	@Test
	void profileCompletionResponseOnlyExposesStatus() {
		assertThrows(
			NoSuchMethodException.class,
			() -> ProfileCompletionDto.Response.class.getMethod("userId")
		);
		assertThrows(
			NoSuchMethodException.class,
			() -> ProfileCompletionDto.Response.class.getMethod("profileCompleted")
		);
		assertThrows(
			NoSuchMethodException.class,
			() -> ProfileCompletionDto.Response.class.getMethod("nextAction")
		);
	}

	@Test
	void completeProfileThrowsNotFoundWhenMemberDoesNotExist() {
		CompleteMemberProfileUseCase completeMemberProfileUseCase = new CompleteMemberProfileUseCase(
			userRepository,
			ensureFinMemberLinkedUseCase
		);
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

	@Test
	void completeProfileKeepsMemberTempWhenFinancialLinkFails() {
		ListAppender<ILoggingEvent> appender = attachAppender();
		CompleteMemberProfileUseCase completeMemberProfileUseCase = new CompleteMemberProfileUseCase(
			userRepository,
			ensureFinMemberLinkedUseCase
		);
		MemberProfileService memberProfileService = new MemberProfileService(completeMemberProfileUseCase);
		User user = User.createPendingSocialUser();
		setUserId(user, 7);
		given(userRepository.findById(7)).willReturn(Optional.of(user));
		given(ensureFinMemberLinkedUseCase.execute(7, "moa@example.com"))
			.willThrow(new MemberException(MemberErrorCode.FINANCIAL_MEMBER_LINK_FAILED));

		MemberException exception = assertThrows(
			MemberException.class,
			() -> memberProfileService.completeProfile(
				7,
				new ProfileCompletionDto.Request(
					"moa",
					"moa@example.com",
					LocalDate.of(1997, 3, 1),
					true
				)
			)
		);

		assertEquals(MemberErrorCode.FINANCIAL_MEMBER_LINK_FAILED, exception.getErrorCode());
		assertEquals("TEMP", user.getStatus().name());
		assertTrue(containsLog(appender, "event=MEMBER_SIGNUP_COMPLETE_FAILURE"));
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

	private ListAppender<ILoggingEvent> attachAppender() {
		Logger logger = (Logger) LoggerFactory.getLogger(MemberProfileService.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		return appender;
	}

	private boolean containsLog(ListAppender<ILoggingEvent> appender, String text) {
		return appender.list.stream()
			.anyMatch(event -> event.getFormattedMessage().contains(text));
	}
}

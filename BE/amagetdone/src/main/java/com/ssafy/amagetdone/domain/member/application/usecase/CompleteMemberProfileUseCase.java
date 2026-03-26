package com.ssafy.amagetdone.domain.member.application.usecase;

import java.time.LocalDate;

import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CompleteMemberProfileUseCase {

	private final UserRepository userRepository;

	public CompleteMemberProfileUseCase(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional
	public User execute(Integer userId, String nickname, String email, LocalDate birthDate, Boolean hasFixedIncome) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new MemberException(MemberErrorCode.USER_NOT_FOUND));

		user.markProfileEntered(nickname, email, birthDate, hasFixedIncome);
		user.activate();
		return user;
	}
}

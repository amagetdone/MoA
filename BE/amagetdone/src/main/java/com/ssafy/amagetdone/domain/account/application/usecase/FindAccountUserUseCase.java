package com.ssafy.amagetdone.domain.account.application.usecase;

import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.member.application.usecase.EnsureFinMemberLinkedUseCase;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;

import org.springframework.stereotype.Component;

@Component
public class FindAccountUserUseCase {

	private final UserRepository userRepository;
	private final EnsureFinMemberLinkedUseCase ensureFinMemberLinkedUseCase;

	public FindAccountUserUseCase(
		UserRepository userRepository,
		EnsureFinMemberLinkedUseCase ensureFinMemberLinkedUseCase
	) {
		this.userRepository = userRepository;
		this.ensureFinMemberLinkedUseCase = ensureFinMemberLinkedUseCase;
	}

	public User execute(Integer userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new AccountException(AccountErrorCode.USER_NOT_FOUND));
		if (user.getUserKey() == null || user.getUserKey().isBlank()) {
			if (user.getEmail() == null || user.getEmail().isBlank()) {
				throw new AccountException(AccountErrorCode.USER_KEY_REQUIRED);
			}
			String userKey = ensureFinMemberLinkedUseCase.execute(userId, user.getEmail());
			user.updateUserKey(userKey);
		}
		return user;
	}
}

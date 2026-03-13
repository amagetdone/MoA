package com.ssafy.amagetdone.domain.account.application.usecase;

import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;

import org.springframework.stereotype.Component;

@Component
public class FindAccountUserUseCase {

	private final UserRepository userRepository;

	public FindAccountUserUseCase(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public User execute(Integer userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new AccountException(AccountErrorCode.USER_NOT_FOUND));
		if (user.getUserKey() == null || user.getUserKey().isBlank()) {
			throw new AccountException(AccountErrorCode.USER_KEY_REQUIRED);
		}
		return user;
	}
}

package com.ssafy.amagetdone.domain.account.application.usecase;

import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;

import org.springframework.stereotype.Component;

@Component
public class FindAccountUserByUserKeyUseCase {

	private final UserRepository userRepository;

	public FindAccountUserByUserKeyUseCase(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public User execute(String userKey) {
		if (userKey == null || userKey.isBlank()) {
			throw new AccountException(AccountErrorCode.USER_KEY_REQUIRED);
		}
		return userRepository.findByUserKey(userKey)
			.orElseThrow(() -> new AccountException(AccountErrorCode.USER_NOT_FOUND));
	}
}

package com.ssafy.amagetdone.domain.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindAccountUserByUserKeyUseCaseTest {

	@Mock
	private UserRepository userRepository;

	@Test
	void executeReturnsUserWhenUserKeyMatches() {
		FindAccountUserByUserKeyUseCase useCase = new FindAccountUserByUserKeyUseCase(userRepository);
		User user = User.builder().id(1).userKey("user-key").build();
		given(userRepository.findByUserKey("user-key")).willReturn(Optional.of(user));

		User result = useCase.execute("user-key");

		assertThat(result).isSameAs(user);
	}

	@Test
	void executeThrowsWhenUserKeyIsBlank() {
		FindAccountUserByUserKeyUseCase useCase = new FindAccountUserByUserKeyUseCase(userRepository);

		assertThatThrownBy(() -> useCase.execute(" "))
			.isInstanceOf(AccountException.class)
			.extracting(exception -> ((AccountException)exception).getErrorCode())
			.isEqualTo(AccountErrorCode.USER_KEY_REQUIRED);
	}

	@Test
	void executeThrowsWhenUserDoesNotExist() {
		FindAccountUserByUserKeyUseCase useCase = new FindAccountUserByUserKeyUseCase(userRepository);
		given(userRepository.findByUserKey("missing-key")).willReturn(Optional.empty());

		assertThatThrownBy(() -> useCase.execute("missing-key"))
			.isInstanceOf(AccountException.class)
			.extracting(exception -> ((AccountException)exception).getErrorCode())
			.isEqualTo(AccountErrorCode.USER_NOT_FOUND);
	}
}

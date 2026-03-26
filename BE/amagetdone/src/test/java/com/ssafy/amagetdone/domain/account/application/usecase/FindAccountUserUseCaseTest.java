package com.ssafy.amagetdone.domain.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.member.application.usecase.EnsureFinMemberLinkedUseCase;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindAccountUserUseCaseTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private EnsureFinMemberLinkedUseCase ensureFinMemberLinkedUseCase;

	@Test
	void executeReturnsUserWhenUserKeyAlreadyExists() {
		FindAccountUserUseCase useCase = new FindAccountUserUseCase(userRepository, ensureFinMemberLinkedUseCase);
		User user = User.builder().id(1).email("moa@example.com").userKey("linked-key").build();
		given(userRepository.findById(1)).willReturn(Optional.of(user));

		User result = useCase.execute(1);

		assertThat(result).isSameAs(user);
	}

	@Test
	void executeLinksFinancialMemberWhenUserKeyIsMissing() {
		FindAccountUserUseCase useCase = new FindAccountUserUseCase(userRepository, ensureFinMemberLinkedUseCase);
		User user = User.builder().id(7).email("moa@example.com").build();
		given(userRepository.findById(7)).willReturn(Optional.of(user));
		given(ensureFinMemberLinkedUseCase.execute(7, "moa@example.com")).willReturn("linked-key");

		User result = useCase.execute(7);

		assertThat(result.getUserKey()).isEqualTo("linked-key");
		verify(ensureFinMemberLinkedUseCase).execute(7, "moa@example.com");
	}

	@Test
	void executeThrowsWhenUserKeyAndEmailAreBothMissing() {
		FindAccountUserUseCase useCase = new FindAccountUserUseCase(userRepository, ensureFinMemberLinkedUseCase);
		User user = User.builder().id(3).build();
		given(userRepository.findById(3)).willReturn(Optional.of(user));

		assertThatThrownBy(() -> useCase.execute(3))
			.isInstanceOf(AccountException.class)
			.extracting(exception -> ((AccountException)exception).getErrorCode())
			.isEqualTo(AccountErrorCode.USER_KEY_REQUIRED);
	}
}

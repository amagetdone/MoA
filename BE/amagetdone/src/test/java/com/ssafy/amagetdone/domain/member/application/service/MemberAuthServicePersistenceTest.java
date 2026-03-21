package com.ssafy.amagetdone.domain.member.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.List;

import com.ssafy.amagetdone.domain.member.api.dto.SocialAuthDto;
import com.ssafy.amagetdone.domain.member.api.dto.TestSocialAuthDto;
import com.ssafy.amagetdone.domain.member.application.usecase.EnsureFinMemberLinkedUseCase;
import com.ssafy.amagetdone.domain.member.application.usecase.IssueMemberTokensUseCase;
import com.ssafy.amagetdone.domain.member.application.usecase.ResetTestSocialUserUseCase;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;
import com.ssafy.amagetdone.domain.member.support.util.MemberTokenPair;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MemberAuthServicePersistenceTest {

	@Autowired
	private MemberAuthService memberAuthService;

	@Autowired
	private UserRepository userRepository;

	@MockBean
	private EnsureFinMemberLinkedUseCase ensureFinMemberLinkedUseCase;

	@MockBean
	private IssueMemberTokensUseCase issueMemberTokensUseCase;

	@MockBean
	private ResetTestSocialUserUseCase resetTestSocialUserUseCase;

	@Test
	void authenticateTestPersistsResolvedUserKey() {
		given(ensureFinMemberLinkedUseCase.execute(anyInt(), anyString())).willReturn("persisted-user-key");
		given(issueMemberTokensUseCase.execute(any(User.class), eq(SocialProvider.TEST)))
			.willReturn(new MemberTokenPair("access-token", "refresh-token"));

		SocialAuthDto.Response response = memberAuthService.authenticateTest(new TestSocialAuthDto.Request(1));

		List<User> users = userRepository.findAll();
		assertEquals(1, users.size());
		assertEquals("persisted-user-key", users.get(0).getUserKey());
		assertFalse(response.account());
	}
}

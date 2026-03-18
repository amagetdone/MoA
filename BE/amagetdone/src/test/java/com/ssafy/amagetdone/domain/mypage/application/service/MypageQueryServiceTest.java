package com.ssafy.amagetdone.domain.mypage.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

import java.lang.reflect.Field;
import java.util.Optional;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.domain.mypage.api.dto.MypageDto;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MypageQueryServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PlanRepository planRepository;

	@Test
	void getMypageCombinesUserProfileAndPlanSummary() {
		MypageQueryService mypageQueryService = new MypageQueryService(userRepository, planRepository);
		User user = User.createPendingSocialUser();
		setUserId(user, 1);
		user.setNickname("테스트유저");
		user.setEmail("test@more.com");
		user.setProfileUrl("profile/test.png");

		given(userRepository.findById(1)).willReturn(Optional.of(user));
		given(planRepository.countByUser_IdAndStatus(1, PlanStatus.ACTIVE)).willReturn(4L);
		given(planRepository.countByUser_IdAndStatus(1, PlanStatus.COMPLETED)).willReturn(1L);
		given(planRepository.sumSavedAmountByUserId(1)).willReturn(5690000L);

		MypageDto.Response response = mypageQueryService.getMypage(1);

		assertEquals("테스트유저", response.userName());
		assertEquals("test@more.com", response.userEmail());
		assertEquals("profile/test.png", response.profileImageUrl());
		assertEquals(4L, response.activePlanCount());
		assertEquals(1L, response.completedPlanCount());
		assertEquals(5690000L, response.totalSavedAmount());
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

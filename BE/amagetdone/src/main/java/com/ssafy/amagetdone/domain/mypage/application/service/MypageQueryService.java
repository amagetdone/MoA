package com.ssafy.amagetdone.domain.mypage.application.service;

import java.util.Optional;

import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.domain.mypage.api.dto.MypageDto;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MypageQueryService {

	private final UserRepository userRepository;
	private final PlanRepository planRepository;

	public MypageDto.Response getMypage(Integer userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new MemberException(MemberErrorCode.USER_NOT_FOUND));

		long activePlanCount = planRepository.countByUser_IdAndStatusAndDeletedFalse(userId, PlanStatus.ACTIVE);
		long completedPlanCount = planRepository.countByUser_IdAndStatusAndDeletedFalse(userId, PlanStatus.COMPLETED);
		long totalSavedAmount = Optional.ofNullable(planRepository.sumSavedAmountByUserIdAndDeletedFalse(userId)).orElse(0L);

		return new MypageDto.Response(
			user.getNickname(),
			user.getEmail(),
			user.getProfileUrl(),
			activePlanCount,
			completedPlanCount,
			totalSavedAmount
		);
	}
}

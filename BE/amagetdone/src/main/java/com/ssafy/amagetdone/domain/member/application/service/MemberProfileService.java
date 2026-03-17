package com.ssafy.amagetdone.domain.member.application.service;

import com.ssafy.amagetdone.domain.member.api.dto.ProfileCompletionDto;
import com.ssafy.amagetdone.domain.member.application.usecase.CompleteMemberProfileUseCase;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.support.log.MemberLogHelper;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberProfileService {

	private static final Logger log = LoggerFactory.getLogger(MemberProfileService.class);

	private final CompleteMemberProfileUseCase completeMemberProfileUseCase;

	public ProfileCompletionDto.Response completeProfile(Integer userId, ProfileCompletionDto.Request request) {
		String requestId = MemberLogHelper.requestId();
		String maskedEmail = MemberLogHelper.maskEmail(request.email());
		try {
			User user = completeMemberProfileUseCase.execute(
				userId,
				request.nickname(),
				request.email(),
				request.birthDate(),
				request.hasFixedIncome()
			);

			log.info(
				"event=MEMBER_SIGNUP_COMPLETE_SUCCESS requestId={} localUserId={} userStatus={} userIdMasked={}",
				requestId,
				user.getId(),
				user.getStatus().name(),
				maskedEmail
			);

			return new ProfileCompletionDto.Response(user.getStatus().name());
		} catch (RuntimeException exception) {
			log.warn(
				"event=MEMBER_SIGNUP_COMPLETE_FAILURE requestId={} localUserId={} userIdMasked={} errorType={}",
				requestId,
				userId,
				maskedEmail,
				exception.getClass().getSimpleName()
			);
			throw exception;
		}
	}
}

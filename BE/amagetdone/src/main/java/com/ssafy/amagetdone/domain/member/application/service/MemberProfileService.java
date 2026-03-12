package com.ssafy.amagetdone.domain.member.application.service;

import com.ssafy.amagetdone.domain.member.api.dto.ProfileCompletionDto;
import com.ssafy.amagetdone.domain.member.application.usecase.CompleteMemberProfileUseCase;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.springframework.stereotype.Service;

@Service
public class MemberProfileService {

	private final CompleteMemberProfileUseCase completeMemberProfileUseCase;

	public MemberProfileService(CompleteMemberProfileUseCase completeMemberProfileUseCase) {
		this.completeMemberProfileUseCase = completeMemberProfileUseCase;
	}

	public ProfileCompletionDto.Response completeProfile(Integer userId, ProfileCompletionDto.Request request) {
		User user = completeMemberProfileUseCase.execute(
			userId,
			request.nickname(),
			request.email(),
			request.birthDate(),
			request.hasFixedIncome()
		);
		boolean profileCompleted = user.isProfileCompleted();

		return new ProfileCompletionDto.Response(
			user.getId(),
			profileCompleted,
			user.getStatus(),
			profileCompleted ? "GO_HOME" : "COMPLETE_PROFILE"
		);
	}
}

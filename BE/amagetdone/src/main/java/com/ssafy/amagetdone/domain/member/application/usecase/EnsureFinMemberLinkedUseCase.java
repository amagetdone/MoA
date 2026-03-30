package com.ssafy.amagetdone.domain.member.application.usecase;

import java.util.List;

import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.infra.client.SsafyFinMemberClient;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;

import org.springframework.stereotype.Component;

@Component
public class EnsureFinMemberLinkedUseCase {

	private final SsafyFinMemberClient ssafyFinMemberClient;
	private final UserRepository userRepository;

	public EnsureFinMemberLinkedUseCase(SsafyFinMemberClient ssafyFinMemberClient, UserRepository userRepository) {
		this.ssafyFinMemberClient = ssafyFinMemberClient;
		this.userRepository = userRepository;
	}

	public String execute(Integer currentUserId, String email) {
		String userKey = resolveUserKey(email);
		validateOwnership(currentUserId, userKey);
		return userKey;
	}

	private String resolveUserKey(String email) {
		try {
			return ssafyFinMemberClient.createMember(email).userKey();
		} catch (MemberException exception) {
			if (exception.getErrorCode() != MemberErrorCode.FINANCIAL_MEMBER_ALREADY_EXISTS) {
				throw exception;
			}
		}

		return ssafyFinMemberClient.searchMember(email).userKey();
	}

	private void validateOwnership(Integer currentUserId, String userKey) {
		List<User> owners = userRepository.findAllByUserKey(userKey);
		if (owners.isEmpty()) {
			return;
		}
		boolean currentUserOwnsKey = owners.stream()
			.anyMatch(owner -> owner.getId().equals(currentUserId));
		if (currentUserOwnsKey) {
			return;
		}
		throw new MemberException(MemberErrorCode.FINANCIAL_MEMBER_ALREADY_LINKED);
	}
}

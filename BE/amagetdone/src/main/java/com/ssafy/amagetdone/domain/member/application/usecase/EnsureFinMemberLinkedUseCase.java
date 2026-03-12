package com.ssafy.amagetdone.domain.member.application.usecase;

import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.infra.client.SsafyFinMemberClient;

import org.springframework.stereotype.Component;

@Component
public class EnsureFinMemberLinkedUseCase {

	private final SsafyFinMemberClient ssafyFinMemberClient;

	public EnsureFinMemberLinkedUseCase(SsafyFinMemberClient ssafyFinMemberClient) {
		this.ssafyFinMemberClient = ssafyFinMemberClient;
	}

	public String execute(String email) {
		try {
			return ssafyFinMemberClient.createMember(email).userKey();
		} catch (MemberException exception) {
			if (exception.getErrorCode() != MemberErrorCode.FINANCIAL_MEMBER_ALREADY_EXISTS) {
				throw exception;
			}
		}

		return ssafyFinMemberClient.searchMember(email).userKey();
	}
}

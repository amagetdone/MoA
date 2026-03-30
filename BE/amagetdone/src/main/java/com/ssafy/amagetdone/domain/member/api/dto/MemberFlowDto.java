package com.ssafy.amagetdone.domain.member.api.dto;

public final class MemberFlowDto {

	private MemberFlowDto() {
	}

	public record ResetResponse(
		boolean success
	) {
	}
}

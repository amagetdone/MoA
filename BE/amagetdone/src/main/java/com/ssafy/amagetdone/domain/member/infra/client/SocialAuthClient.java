package com.ssafy.amagetdone.domain.member.infra.client;

import com.ssafy.amagetdone.domain.member.api.dto.SocialAuthDto;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;

public interface SocialAuthClient {

	boolean supports(SocialProvider provider);

	SocialUserIdentity fetchIdentity(SocialAuthDto.Request request);
}

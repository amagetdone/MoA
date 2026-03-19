package com.ssafy.amagetdone.domain.mypage.application.usecase;

import com.ssafy.amagetdone.domain.mypage.api.dto.MypageDto;
import com.ssafy.amagetdone.domain.mypage.application.service.MypageQueryService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetMypageUseCase {

	private final MypageQueryService mypageQueryService;

	public MypageDto.Response execute(Integer userId) {
		return mypageQueryService.getMypage(userId);
	}
}

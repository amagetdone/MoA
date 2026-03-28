package com.ssafy.amagetdone.domain.plan.application.usecase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.domain.plan.exception.PlanErrorCode;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanAiPipelineStatus;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;
import com.ssafy.amagetdone.domain.plan.model.vo.PyanTaskStatus;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InitializePlanDataUseCase {

	private static final List<PlanSeed> PLAN_SEEDS = List.of(
		new PlanSeed(
			"맥북 프로 구매",
			null,
			900_000L,
			900_000L,
			LocalDate.of(2025, 12, 27),
			LocalDate.of(2026, 3, 27),
			1,
			PlanStatus.COMPLETED,
			true,
			"plan/b8aed81e-6e51-4f3e-a41f-6a1d9579174c.png",
			"https://sketch.pyan.kr/cdn/21ba6839-6a3a-4004-b03d-a0b4291515af.compressed.mp4",
			"plan-1-pyan-1",
			PyanTaskStatus.READY,
			"https://sketch.pyan.kr/api/status/21ba6839-6a3a-4004-b03d-a0b4291515af",
			PlanAiPipelineStatus.SUCCEEDED
		),
		new PlanSeed(
			"여행 자금 모으기",
			null,
			600_000L,
			408_791L,
			LocalDate.of(2026, 1, 27),
			LocalDate.of(2026, 4, 27),
			2,
			PlanStatus.ACTIVE,
			true,
			"plan/ad8345b4-905b-4c7b-a876-ec93a3fe6120.png",
			"https://sketch.pyan.kr/cdn/67c603d2-308c-4de3-96b2-71a551a5052f.compressed.mp4",
			"plan-2-pyan-1",
			PyanTaskStatus.READY,
			"https://sketch.pyan.kr/api/status/67c603d2-308c-4de3-96b2-71a551a5052f",
			PlanAiPipelineStatus.SUCCEEDED
		),
		new PlanSeed(
			"자격증 교재 구매",
			null,
			300_000L,
			102_198L,
			LocalDate.of(2026, 2, 27),
			LocalDate.of(2026, 5, 27),
			3,
			PlanStatus.ACTIVE,
			true,
			"plan/2845ace7-1ef2-4981-9de9-b6500624b02e.png",
			"https://sketch.pyan.kr/cdn/4df51379-d3de-45c6-bef0-be32e2efb474.compressed.mp4",
			"plan-3-pyan-1",
			PyanTaskStatus.READY,
			"https://sketch.pyan.kr/api/status/4df51379-d3de-45c6-bef0-be32e2efb474",
			PlanAiPipelineStatus.SUCCEEDED
		)
	);

	private final UserRepository userRepository;
	private final PlanRepository planRepository;

	public InitializePlanDataUseCase(
		UserRepository userRepository,
		PlanRepository planRepository
	) {
		this.userRepository = userRepository;
		this.planRepository = planRepository;
	}

	@Transactional
	public AccountAdminDto.PlanDataInitResponse execute(AccountAdminDto.PlanDataInitRequest request) {
		Integer userId = request == null ? null : request.userId();
		if (userId == null || userId <= 0) {
			throw new PlanException(PlanErrorCode.INVALID_PLAN_DATA_INIT_REQUEST);
		}

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new MemberException(MemberErrorCode.USER_NOT_FOUND));

		List<String> createdTitles = new ArrayList<>();
		List<String> skippedTitles = new ArrayList<>();
		List<Plan> createdPlans = new ArrayList<>();

		for (PlanSeed seed : PLAN_SEEDS) {
			if (planRepository.existsByUserIdAndTitleAndDeletedFalse(userId, seed.title())) {
				skippedTitles.add(seed.title());
				continue;
			}
			createdPlans.add(buildPlan(user, seed));
			createdTitles.add(seed.title());
		}

		if (!createdPlans.isEmpty()) {
			planRepository.saveAll(createdPlans);
		}

		return new AccountAdminDto.PlanDataInitResponse(
			"plan",
			userId,
			createdTitles.size(),
			skippedTitles.size(),
			List.copyOf(createdTitles),
			List.copyOf(skippedTitles)
		);
	}

	private Plan buildPlan(User user, PlanSeed seed) {
		Plan plan = Plan.create(
			user,
			seed.title(),
			seed.description(),
			seed.targetAmount(),
			seed.startDate(),
			seed.endDate(),
			seed.priority()
		);
		plan.setSavedAmount(seed.savedAmount());
		plan.setStatus(seed.status());
		plan.setAutoAllocateEnabled(seed.autoAllocateEnabled());
		plan.setPlanUrl(seed.planUrl());
		plan.setVideoUrl(seed.videoUrl());
		plan.setPyanRequestKey(seed.pyanRequestKey());
		plan.setPyanStatus(seed.pyanStatus());
		plan.setPyanStatusUrl(seed.pyanStatusUrl());
		plan.setAiPipelineStatus(seed.aiPipelineStatus());
		plan.setPyanLastError(null);
		return plan;
	}

	private record PlanSeed(
		String title,
		String description,
		Long targetAmount,
		Long savedAmount,
		LocalDate startDate,
		LocalDate endDate,
		Integer priority,
		PlanStatus status,
		boolean autoAllocateEnabled,
		String planUrl,
		String videoUrl,
		String pyanRequestKey,
		PyanTaskStatus pyanStatus,
		String pyanStatusUrl,
		PlanAiPipelineStatus aiPipelineStatus
	) {
	}
}

package com.ssafy.amagetdone.domain.plan.application.service;

import com.ssafy.amagetdone.domain.plan.exception.PlanErrorCode;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.global.infra.storage.ImageCategory;
import com.ssafy.amagetdone.global.infra.storage.S3StorageService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PlanImageService {

	private final PlanRepository planRepository;
	private final S3StorageService storageService;

	public PlanImageService(PlanRepository planRepository, S3StorageService storageService) {
		this.planRepository = planRepository;
		this.storageService = storageService;
	}

	@Transactional
	public String replacePlanImage(Integer userId, Integer planId, MultipartFile file) {
		Plan plan = planRepository.findByIdAndUserId(planId, userId)
			.orElseThrow(() -> new PlanException(PlanErrorCode.PLAN_NOT_FOUND));

		if (hasText(plan.getPlanUrl())) {
			storageService.delete(plan.getPlanUrl());
		}

		String objectKey = storageService.upload(file, ImageCategory.PLAN);
		plan.updatePlanUrl(objectKey);
		return objectKey;
	}

	public byte[] loadPlanImageBytes(Integer userId, Integer planId) {
		Plan plan = planRepository.findByIdAndUserId(planId, userId)
			.orElseThrow(() -> new PlanException(PlanErrorCode.PLAN_NOT_FOUND));
		if (!hasText(plan.getPlanUrl())) {
			throw new IllegalArgumentException("Plan image is missing.");
		}
		return storageService.loadBytes(plan.getPlanUrl());
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}

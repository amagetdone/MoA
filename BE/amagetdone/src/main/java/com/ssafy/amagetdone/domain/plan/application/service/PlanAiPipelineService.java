package com.ssafy.amagetdone.domain.plan.application.service;

import com.ssafy.amagetdone.domain.member.application.service.MemberProfileImageService;
import com.ssafy.amagetdone.domain.plan.exception.PlanErrorCode;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.domain.plan.infra.client.PlanAiClient;
import com.ssafy.amagetdone.domain.plan.infra.client.PyanClient;
import com.ssafy.amagetdone.domain.plan.infra.client.dto.PyanGenerateResponse;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.global.infra.storage.ImageCategory;
import com.ssafy.amagetdone.global.infra.storage.S3StorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
@ConditionalOnBean({ PlanAiClient.class, PyanClient.class })
public class PlanAiPipelineService {

	private static final Logger log = LoggerFactory.getLogger(PlanAiPipelineService.class);

	private final PlanRepository planRepository;
	private final MemberProfileImageService memberProfileImageService;
	private final PlanAiClient planAiClient;
	private final S3StorageService storageService;
	private final PyanClient pyanClient;

	public PlanAiPipelineService(
		PlanRepository planRepository,
		MemberProfileImageService memberProfileImageService,
		PlanAiClient planAiClient,
		S3StorageService storageService,
		PyanClient pyanClient
	) {
		this.planRepository = planRepository;
		this.memberProfileImageService = memberProfileImageService;
		this.planAiClient = planAiClient;
		this.storageService = storageService;
		this.pyanClient = pyanClient;
	}

	@Transactional
	public void start(Integer planId, Integer userId) {
		Plan plan = planRepository.findByIdAndUserId(planId, userId)
			.orElseThrow(() -> new PlanException(PlanErrorCode.PLAN_NOT_FOUND));

		try {
			plan.markAiProcessing();
			planRepository.saveAndFlush(plan);
			log.info("event=PLAN_AI_PIPELINE_STARTED planId={} userId={} title={}", planId, userId, plan.getTitle());
			String profileImageUrl = memberProfileImageService.loadProfileImageUrl(userId);
			String userPrompt = buildUserPrompt(plan);
			log.info(
				"event=PLAN_AI_REQUEST_PREPARED planId={} userId={} profileImageUrl={} userPrompt={}",
				planId,
				userId,
				profileImageUrl,
				userPrompt
			);
			String generatedImageBase64 = planAiClient.generatePlanImage(userPrompt, profileImageUrl);
			byte[] generatedImageBytes = decodeBase64Image(generatedImageBase64);
			log.info(
				"event=PLAN_AI_RESPONSE_DECODED planId={} userId={} imageBytesLength={}",
				planId,
				userId,
				generatedImageBytes.length
			);
			String objectKey = storageService.upload(
				new ByteArrayMultipartFile("image", "plan.png", "image/png", generatedImageBytes),
				ImageCategory.PLAN
			);
			plan.updatePlanUrl(objectKey);
			planRepository.saveAndFlush(plan);
			log.info("event=PLAN_IMAGE_STORED planId={} userId={} objectKey={}", planId, userId, objectKey);

			String goalId = "plan-" + plan.getId();
			log.info("event=PYAN_REQUEST_STARTED planId={} userId={} goalId={}", planId, userId, goalId);
			PyanGenerateResponse response = pyanClient.generate(generatedImageBytes, goalId);
			String videoUrl = resolveCompressedDownloadUrl(response);
			plan.updateVideoUrl(videoUrl);
			plan.markAiSucceeded();
			planRepository.saveAndFlush(plan);
			log.info(
				"event=PLAN_AI_PIPELINE_SUCCESS planId={} userId={} planImageKey={} videoUrl={}",
				planId,
				userId,
				objectKey,
				videoUrl
			);
		} catch (Exception exception) {
			plan.markAiFailed();
			planRepository.saveAndFlush(plan);
			log.error("event=PLAN_AI_PIPELINE_FAILED planId={} userId={} message={}", planId, userId,
				exception.getMessage(), exception);
		}
	}

	private byte[] decodeBase64Image(String base64Image) {
		if (base64Image == null || base64Image.isBlank()) {
			throw new IllegalArgumentException("Generated image is missing.");
		}
		String encoded = base64Image;
		int commaIndex = base64Image.indexOf(',');
		if (base64Image.startsWith("data:") && commaIndex >= 0) {
			encoded = base64Image.substring(commaIndex + 1);
		}
		return Base64.getDecoder().decode(encoded);
	}

	private String buildUserPrompt(Plan plan) {
		String description = plan.getDescription() == null ? "" : plan.getDescription();
		return "[Title] " + plan.getTitle() + " [description] " + description;
	}

	private String resolveCompressedDownloadUrl(PyanGenerateResponse response) {
		if (response == null || response.compressedDownloadUrl() == null || response.compressedDownloadUrl().isBlank()) {
			throw new IllegalStateException("Pyan response does not contain compressed download URL");
		}
		return response.compressedDownloadUrl();
	}

	private static final class ByteArrayMultipartFile implements MultipartFile {
		private final String name;
		private final String originalFilename;
		private final String contentType;
		private final byte[] content;

		private ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
			this.name = name;
			this.originalFilename = originalFilename;
			this.contentType = contentType;
			this.content = content;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getOriginalFilename() {
			return originalFilename;
		}

		@Override
		public String getContentType() {
			return contentType;
		}

		@Override
		public boolean isEmpty() {
			return content.length == 0;
		}

		@Override
		public long getSize() {
			return content.length;
		}

		@Override
		public byte[] getBytes() {
			return content.clone();
		}

		@Override
		public InputStream getInputStream() {
			return new ByteArrayInputStream(content);
		}

		@Override
		public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
			throw new UnsupportedOperationException("transferTo is not supported");
		}
	}
}

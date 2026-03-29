package com.ssafy.amagetdone.domain.plan.infra.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PyanGenerateResponse(
	@JsonProperty("status") String status,
	@JsonProperty("task_id") String taskId,
	@JsonProperty("goal_id") String goalId,
	@JsonProperty("message") String message,
	@JsonProperty("status_url") String statusUrl,
	@JsonProperty("video_url") String videoUrl,
	@JsonProperty("original_video_url") String originalVideoUrl,
	@JsonProperty("compressed_video_url") String compressedVideoUrl,
	@JsonProperty("download_url") String downloadUrl,
	@JsonProperty("goal_status_url") String goalStatusUrl,
	@JsonProperty("original_download_url") String originalDownloadUrl,
	@JsonProperty("compressed_download_url") String compressedDownloadUrl,
	@JsonProperty("user_id") Integer userId,
	@JsonProperty("plan_id") Integer planId,
	@JsonProperty("plan_title") String planTitle,
	@JsonProperty("demo_fallback_eligible") Boolean demoFallbackEligible,
	@JsonProperty("demo_fallback_scheduled") Boolean demoFallbackScheduled,
	@JsonProperty("demo_fallback_applied") Boolean demoFallbackApplied,
	@JsonProperty("demo_fallback_reason") String demoFallbackReason,
	@JsonProperty("demo_fallback_asset_url") String demoFallbackAssetUrl
) {
}

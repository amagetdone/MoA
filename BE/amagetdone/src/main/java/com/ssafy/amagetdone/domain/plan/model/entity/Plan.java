package com.ssafy.amagetdone.domain.plan.model.entity;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanAiPipelineStatus;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;
import com.ssafy.amagetdone.domain.plan.model.vo.PyanTaskStatus;
import com.ssafy.amagetdone.global.entity.BaseEntity;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "plan")
public class Plan extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "users_id", nullable = false)
	private User user;

	@Column(name = "title", nullable = false, length = 100)
	private String title;

	@Column(name = "description", length = 255)
	private String description;

	@Column(name = "target_amount", nullable = false)
	private Long targetAmount;

	@ColumnDefault("0")
	@Column(name = "saved_amount", nullable = false)
	@Builder.Default
	private Long savedAmount = 0L;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "target_weeks", nullable = false)
	private Integer targetWeeks;

	@Column(name = "priority", nullable = false)
	private Integer priority;

	@Builder.Default
	@ColumnDefault("'WAITING'")
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private PlanStatus status = PlanStatus.WAITING;

	@Builder.Default
	@ColumnDefault("TRUE")
	@Column(name = "auto_allocate_enabled", nullable = false)
	private boolean autoAllocateEnabled = true;

	@Column(name = "plan_img_url", length = 255)
	private String planUrl;

	@Column(name = "plan_video_url", length = 255)
	private String videoUrl;

	@Column(name = "pyan_task_id", length = 100)
	private String pyanTaskId;

	@Column(name = "pyan_request_key", length = 120)
	private String pyanRequestKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "pyan_status", length = 30)
	private PyanTaskStatus pyanStatus;

	@Column(name = "pyan_status_url", length = 255)
	private String pyanStatusUrl;

	@Column(name = "pyan_goal_status_url", length = 255)
	private String pyanGoalStatusUrl;

	@Column(name = "pyan_original_download_url", length = 255)
	private String pyanOriginalDownloadUrl;

	@Column(name = "pyan_compressed_download_url", length = 255)
	private String pyanCompressedDownloadUrl;

	@Column(name = "pyan_last_error", length = 255)
	private String pyanLastError;

	@ColumnDefault("'PENDING'")
	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(name = "ai_pipeline_status", nullable = false, length = 30)
	private PlanAiPipelineStatus aiPipelineStatus = PlanAiPipelineStatus.PENDING;

	@ColumnDefault("0")
	@Builder.Default
	@Column(name = "ai_retry_count", nullable = false)
	private Integer aiRetryCount = 0;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Version
	@ColumnDefault("0")
	@Column(name = "version", nullable = false)
	private Long version;


	public static Plan create(
		User user,
		String title,
		String description,
		Long targetAmount,
		LocalDate startDate,
		LocalDate endDate,
		Integer priority
	) {
		Plan plan = new Plan();
		plan.user = user;
		plan.title = title;
		plan.description = description;
		plan.targetAmount = targetAmount;
		plan.startDate = startDate;
		plan.endDate = endDate;
		plan.priority = priority;
		plan.targetWeeks = calculateTargetWeeks(startDate, endDate);
		return plan;
	}

	private static int calculateTargetWeeks(LocalDate startDate, LocalDate endDate) {
		if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
			return 0;
		}
		long durationDays = ChronoUnit.DAYS.between(startDate, endDate) + 1L;
		return (int)((durationDays + 6L) / 7L);
	}

	public void updatePlanUrl(String planUrl) {
		this.planUrl = planUrl;
	}

	public void updateVideoUrl(String videoUrl) {
		this.videoUrl = videoUrl;
	}

	public void shiftPriority(int delta) {
		this.priority += delta;
	}

	public void updatePriority(int priority) {
		this.priority = priority;
	}

	public void activate() {
		this.status = PlanStatus.ACTIVE;
	}

	public void cancelAndDelete() {
		this.status = PlanStatus.CANCELLED;
		this.setDeleted(true);
	}

	public void extendEndDate(int additionalWeeks) {
		if (additionalWeeks <= 0) {
			return;
		}
		this.endDate = this.endDate.plusWeeks(additionalWeeks);
		this.targetWeeks = calculateTargetWeeks(this.startDate, this.endDate);
	}

	public void markAiProcessing() {
		this.aiPipelineStatus = PlanAiPipelineStatus.PROCESSING;
		this.pyanLastError = null;
	}

	public void assignPyanRequestKey(String requestKey) {
		if (this.pyanRequestKey == null || this.pyanRequestKey.isBlank()) {
			this.pyanRequestKey = requestKey;
		}
	}

	public void registerPyanTask(
		String taskId,
		PyanTaskStatus status,
		String statusUrl,
		String goalStatusUrl,
		String originalDownloadUrl,
		String compressedDownloadUrl
	) {
		this.pyanTaskId = taskId;
		this.pyanStatus = status;
		this.pyanStatusUrl = statusUrl;
		this.pyanGoalStatusUrl = goalStatusUrl;
		this.pyanOriginalDownloadUrl = originalDownloadUrl;
		this.pyanCompressedDownloadUrl = compressedDownloadUrl;
		this.pyanLastError = null;
	}

	public void syncPyanTask(
		PyanTaskStatus status,
		String originalDownloadUrl,
		String compressedDownloadUrl,
		String lastError
	) {
		this.pyanStatus = status;
		if (originalDownloadUrl != null && !originalDownloadUrl.isBlank()) {
			this.pyanOriginalDownloadUrl = originalDownloadUrl;
		}
		if (compressedDownloadUrl != null && !compressedDownloadUrl.isBlank()) {
			this.pyanCompressedDownloadUrl = compressedDownloadUrl;
		}
		this.pyanLastError = lastError;
	}

	public void markAiSucceeded(String videoUrl) {
		this.aiPipelineStatus = PlanAiPipelineStatus.SUCCEEDED;
		this.videoUrl = videoUrl;
		this.pyanLastError = null;
	}

	public void markAiFailed(String lastError) {
		this.aiPipelineStatus = PlanAiPipelineStatus.FAILED;
		this.pyanLastError = lastError;
		this.aiRetryCount++;
	}
}

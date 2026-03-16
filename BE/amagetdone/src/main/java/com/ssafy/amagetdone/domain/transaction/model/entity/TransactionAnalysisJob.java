package com.ssafy.amagetdone.domain.transaction.model.entity;

import java.time.LocalDateTime;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionAnalysisJobStatus;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionAnalysisStep;
import com.ssafy.amagetdone.global.entity.BaseEntity;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "transaction_analysis_job")
public class TransactionAnalysisJob extends BaseEntity {

	@Id
	@Column(name = "id", nullable = false, length = 80)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "users_id", nullable = false)
	private User user;

	@Column(name = "requested_months", nullable = false)
	private Integer requestedMonths;

	@Builder.Default
	@ColumnDefault("'PENDING'")
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private TransactionAnalysisJobStatus status = TransactionAnalysisJobStatus.PENDING;

	@Builder.Default
	@ColumnDefault("'SYNCING'")
	@Enumerated(EnumType.STRING)
	@Column(name = "current_step", nullable = false, length = 30)
	private TransactionAnalysisStep currentStep = TransactionAnalysisStep.SYNCING;

	@Column(name = "requested_at", nullable = false)
	private LocalDateTime requestedAt;

	@Column(name = "started_at")
	private LocalDateTime startedAt;

	@Column(name = "finished_at")
	private LocalDateTime finishedAt;

	@Column(name = "error_code", length = 50)
	private String errorCode;

	@Column(name = "error_message", length = 255)
	private String errorMessage;
}

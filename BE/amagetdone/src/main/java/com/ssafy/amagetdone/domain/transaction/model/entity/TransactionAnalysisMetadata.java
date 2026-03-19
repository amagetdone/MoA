package com.ssafy.amagetdone.domain.transaction.model.entity;

import java.time.LocalDateTime;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
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
@Table(name = "transaction_analysis_metadata")
public class TransactionAnalysisMetadata extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "users_id", nullable = false)
	private User user;

	@Column(name = "requested_months", nullable = false)
	private Integer requestedMonths;

	@Column(name = "window_start_at", nullable = false)
	private LocalDateTime windowStartAt;

	@Column(name = "window_end_at", nullable = false)
	private LocalDateTime windowEndAt;

	@Column(name = "transaction_count", nullable = false)
	private Integer transactionCount;

	@Column(name = "latest_transaction_at")
	private LocalDateTime latestTransactionAt;

	@Column(name = "analyzed_at", nullable = false)
	private LocalDateTime analyzedAt;
}

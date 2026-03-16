package com.ssafy.amagetdone.domain.transaction.model.entity;

import java.time.LocalDateTime;

import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionExcludedReason;
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
@Table(name = "transaction_analysis_result")
public class TransactionAnalysisResult extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "transaction_override_id", nullable = false)
	private TransactionOverride transaction;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@Builder.Default
	@ColumnDefault("TRUE")
	@Column(name = "analysis_included", nullable = false)
	private boolean analysisIncluded = true;

	@Enumerated(EnumType.STRING)
	@Column(name = "excluded_reason", length = 30)
	private TransactionExcludedReason excludedReason;

	@Enumerated(EnumType.STRING)
	@Column(name = "expense_scope", length = 20)
	private ExpenseScope expenseScope;

	@Column(name = "raw_label", length = 100)
	private String rawLabel;

	@Column(name = "final_label", length = 100)
	private String finalLabel;

	@Column(name = "confidence")
	private Double confidence;

	@Builder.Default
	@ColumnDefault("FALSE")
	@Column(name = "fallback_applied", nullable = false)
	private boolean fallbackApplied = false;

	@Column(name = "threshold_used")
	private Double thresholdUsed;

	@Column(name = "analyzed_at", nullable = false)
	private LocalDateTime analyzedAt;
}

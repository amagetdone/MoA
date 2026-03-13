package com.ssafy.amagetdone.domain.budget.model.entity;

import java.time.LocalDateTime;

import com.ssafy.amagetdone.domain.budget.model.vo.BudgetRolloverStatus;
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
@Table(name = "budget_rollover")
public class BudgetRollover extends BaseEntity {

	@Id
	@Column(name = "id", nullable = false)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "source_budget_week_id", nullable = false)
	private BudgetWeek sourceBudgetWeek;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "target_budget_week_id", nullable = false)
	private BudgetWeek targetBudgetWeek;

	@Column(name = "rollover_step", nullable = false)
	private Integer rolloverStep;

	@Column(name = "rollover_amount", nullable = false)
	private Long rolloverAmount;

	@Builder.Default
	@ColumnDefault("'APPLIED'")
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private BudgetRolloverStatus status = BudgetRolloverStatus.APPLIED;

	@Column(name = "resolved_at")
	private LocalDateTime resolvedAt;

	@Column(name = "note", length = 255)
	private String note;
}

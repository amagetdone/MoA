package com.ssafy.amagetdone.domain.budget.model.entity;

import java.time.LocalDateTime;

import com.ssafy.amagetdone.domain.budget.model.vo.BudgetRolloverStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "budget_rollover")
public class BudgetRollover {

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

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private BudgetRolloverStatus status;

	@Column(name = "resolved_at")
	private LocalDateTime resolvedAt;

	@Column(name = "note", length = 255)
	private String note;

	protected BudgetRollover() {
	}
}

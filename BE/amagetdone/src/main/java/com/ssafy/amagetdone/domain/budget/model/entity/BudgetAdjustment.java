package com.ssafy.amagetdone.domain.budget.model.entity;

import com.ssafy.amagetdone.domain.budget.model.vo.BudgetAdjustmentType;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;

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
@Table(name = "budget_adjustment")
public class BudgetAdjustment {

	@Id
	@Column(name = "id", nullable = false)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "budget_week_id", nullable = false)
	private BudgetWeek budgetWeek;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "plan_id")
	private Plan plan;

	@Enumerated(EnumType.STRING)
	@Column(name = "adjustment_type", nullable = false)
	private BudgetAdjustmentType adjustmentType;

	@Column(name = "adjustment_amount", nullable = false)
	private Long adjustmentAmount;

	@Column(name = "reason", length = 255)
	private String reason;

	protected BudgetAdjustment() {
	}
}

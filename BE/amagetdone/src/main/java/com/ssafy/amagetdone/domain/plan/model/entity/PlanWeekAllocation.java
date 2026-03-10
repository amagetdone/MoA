package com.ssafy.amagetdone.domain.plan.model.entity;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanAllocationType;

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
@Table(name = "plan_week_allocation")
public class PlanWeekAllocation {

	@Id
	@Column(name = "id", nullable = false)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "budget_week_id", nullable = false)
	private BudgetWeek budgetWeek;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "plan_id", nullable = false)
	private Plan plan;

	@Enumerated(EnumType.STRING)
	@Column(name = "allocation_type", nullable = false, length = 30)
	private PlanAllocationType allocationType;

	@Column(name = "planned_amount", nullable = false)
	private Long plannedAmount;

	@Column(name = "final_amount", nullable = false)
	private Long finalAmount;

	@Column(name = "note", length = 255)
	private String note;

	protected PlanWeekAllocation() {
	}
}

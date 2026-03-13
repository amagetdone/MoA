package com.ssafy.amagetdone.domain.plan.model.entity;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.global.entity.BaseEntity;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "plan_week_allocation")
public class PlanWeekAllocation extends BaseEntity {

	@Id
	@Column(name = "id", nullable = false)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "budget_week_id", nullable = false)
	private BudgetWeek budgetWeek;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "plan_id", nullable = false)
	private Plan plan;

	@Builder.Default
	@ColumnDefault("'AUTO'")
	@Column(name = "allocation_type", nullable = false, length = 30)
	private String allocationType = "AUTO";

	@Builder.Default
	@ColumnDefault("0")
	@Column(name = "planned_amount", nullable = false)
	private Long plannedAmount = 0L;

	@Builder.Default
	@ColumnDefault("0")
	@Column(name = "final_amount", nullable = false)
	private Long finalAmount = 0L;

	@Column(name = "note", length = 255)
	private String note;
}

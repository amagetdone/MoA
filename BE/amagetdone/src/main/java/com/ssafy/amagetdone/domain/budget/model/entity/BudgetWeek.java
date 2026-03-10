package com.ssafy.amagetdone.domain.budget.model.entity;

import java.time.LocalDate;

import com.ssafy.amagetdone.domain.budget.model.vo.BudgetWeekStatus;
import com.ssafy.amagetdone.domain.member.model.entity.User;

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
@Table(name = "budget_week")
public class BudgetWeek {

	@Id
	@Column(name = "id", nullable = false)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "users_id", nullable = false)
	private User user;

	@Column(name = "week_sequence", nullable = false)
	private Integer weekSequence;

	@Column(name = "week_start_date", nullable = false)
	private LocalDate weekStartDate;

	@Column(name = "week_end_date", nullable = false)
	private LocalDate weekEndDate;

	@Column(name = "period_days", nullable = false)
	private Integer periodDays;

	@Column(name = "is_partial", nullable = false)
	private boolean partial;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private BudgetWeekStatus status;

	@Column(name = "planned_income_amount", nullable = false)
	private Long plannedIncomeAmount;

	@Column(name = "planned_essential_amount", nullable = false)
	private Long plannedEssentialAmount;

	@Column(name = "planned_saving_amount", nullable = false)
	private Long plannedSavingAmount;

	@Column(name = "rollover_in_amount", nullable = false)
	private Long rolloverInAmount;

	@Column(name = "manual_adjustment_amount", nullable = false)
	private Long manualAdjustmentAmount;

	@Column(name = "final_free_amount", nullable = false)
	private Long finalFreeAmount;

	@Column(name = "actual_spent_amount", nullable = false)
	private Long actualSpentAmount;

	protected BudgetWeek() {
	}
}

package com.ssafy.amagetdone.domain.budget.model.entity;

import java.time.LocalDate;

import com.ssafy.amagetdone.domain.budget.model.vo.BudgetWeekStatus;
import com.ssafy.amagetdone.domain.member.model.entity.User;
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
import jakarta.persistence.SequenceGenerator;
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
@Table(name = "budget_week")
public class BudgetWeek extends BaseEntity {

	@Id
	@SequenceGenerator(name = "budget_week_seq_gen", sequenceName = "budget_week_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "budget_week_seq_gen")
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

	@Builder.Default
	@ColumnDefault("7")
	@Column(name = "period_days", nullable = false)
	private Integer periodDays = 7;

	@ColumnDefault("FALSE")
	@Column(name = "is_partial", nullable = false)
	private boolean partial;

	@Builder.Default
	@ColumnDefault("'PLANNED'")
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private BudgetWeekStatus status = BudgetWeekStatus.PLANNED;

	@Builder.Default
	@ColumnDefault("0")
	@Column(name = "planned_income_amount", nullable = false)
	private Long plannedIncomeAmount = 0L;

	@Builder.Default
	@ColumnDefault("0")
	@Column(name = "planned_essential_amount", nullable = false)
	private Long plannedEssentialAmount = 0L;

	@Builder.Default
	@ColumnDefault("0")
	@Column(name = "planned_saving_amount", nullable = false)
	private Long plannedSavingAmount = 0L;

	@Builder.Default
	@ColumnDefault("0")
	@Column(name = "rollover_in_amount", nullable = false)
	private Long rolloverInAmount = 0L;

	@Builder.Default
	@ColumnDefault("0")
	@Column(name = "manual_adjustment_amount", nullable = false)
	private Long manualAdjustmentAmount = 0L;

	@Builder.Default
	@ColumnDefault("0")
	@Column(name = "final_free_amount", nullable = false)
	private Long finalFreeAmount = 0L;

	@Builder.Default
	@ColumnDefault("0")
	@Column(name = "actual_spent_amount", nullable = false)
	private Long actualSpentAmount = 0L;
}

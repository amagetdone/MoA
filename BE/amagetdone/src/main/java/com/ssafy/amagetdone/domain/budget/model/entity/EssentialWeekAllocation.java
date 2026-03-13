package com.ssafy.amagetdone.domain.budget.model.entity;

import com.ssafy.amagetdone.domain.asset.model.entity.Category;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;
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
@Table(name = "essential_week_allocation")
public class EssentialWeekAllocation extends BaseEntity {

	@Id
	@Column(name = "essential_week_allocation_id", nullable = false)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "essential_expense_month_id", nullable = false)
	private EssentialExpenseMonth essentialExpenseMonth;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "budget_week_id", nullable = false)
	private BudgetWeek budgetWeek;

	@Enumerated(EnumType.STRING)
	@Column(name = "expense_scope", nullable = false)
	private ExpenseScope expenseScope;

	@Builder.Default
	@ColumnDefault("0")
	@Column(name = "planned_amount", nullable = false)
	private Long plannedAmount = 0L;

	@Builder.Default
	@ColumnDefault("0")
	@Column(name = "actual_spent_amount", nullable = false)
	private Long actualSpentAmount = 0L;

	@Column(name = "note", length = 255)
	private String note;
}

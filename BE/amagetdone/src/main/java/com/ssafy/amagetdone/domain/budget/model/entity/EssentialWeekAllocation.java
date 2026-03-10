package com.ssafy.amagetdone.domain.budget.model.entity;

import com.ssafy.amagetdone.domain.asset.model.entity.Category;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;

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
@Table(name = "essential_week_allocation")
public class EssentialWeekAllocation {

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

	@Column(name = "planned_amount", nullable = false)
	private Long plannedAmount;

	@Column(name = "actual_spent_amount", nullable = false)
	private Long actualSpentAmount;

	@Column(name = "note", length = 255)
	private String note;

	protected EssentialWeekAllocation() {
	}
}

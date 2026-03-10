package com.ssafy.amagetdone.domain.budget.model.entity;

import com.ssafy.amagetdone.domain.asset.model.entity.Category;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;
import com.ssafy.amagetdone.domain.budget.model.vo.SourceProvider;

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
@Table(name = "essential_expense_month")
public class EssentialExpenseMonth {

	@Id
	@Column(name = "id", nullable = false)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "budget_month_id", nullable = false)
	private BudgetMonth budgetMonth;

	@Enumerated(EnumType.STRING)
	@Column(name = "expense_scope", nullable = false, length = 20)
	private ExpenseScope expenseScope;

	@Column(name = "amount", nullable = false)
	private Long amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_provider", nullable = false)
	private SourceProvider sourceProvider;

	protected EssentialExpenseMonth() {
	}
}

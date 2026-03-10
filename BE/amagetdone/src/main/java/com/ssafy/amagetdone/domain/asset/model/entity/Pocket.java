package com.ssafy.amagetdone.domain.asset.model.entity;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "pocket")
public class Pocket {

	@Id
	@Column(name = "id", nullable = false, length = 50)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "budget_week_id", nullable = false)
	private BudgetWeek budgetWeek;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@Column(name = "name", nullable = false, length = 30)
	private String name;

	@Column(name = "allocated_amount", nullable = false)
	private Long allocatedAmount;

	protected Pocket() {
	}
}

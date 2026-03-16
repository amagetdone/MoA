package com.ssafy.amagetdone.domain.budget.model.entity;

import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.budget.model.vo.SourceProvider;
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
@Table(name = "essential_expense_month")
public class EssentialExpenseMonth extends BaseEntity {

	@Id
	@Column(name = "id", nullable = false)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "budget_month_id", nullable = false)
	private BudgetMonth budgetMonth;

	@Column(name = "expense_scope", nullable = false, length = 20)
	private String expenseScope;

	@Column(name = "amount", nullable = false)
	private Long amount;

	@Builder.Default
	@ColumnDefault("'ANALYZER'")
	@Enumerated(EnumType.STRING)
	@Column(name = "source_provider", nullable = false)
	private SourceProvider sourceProvider = SourceProvider.ANALYZER;
}

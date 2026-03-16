package com.ssafy.amagetdone.domain.budget.model.entity;

import java.time.LocalDate;

import com.ssafy.amagetdone.domain.budget.model.vo.BudgetMonthStatus;
import com.ssafy.amagetdone.domain.member.model.entity.User;
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
@Table(name = "budget_month")
public class BudgetMonth extends BaseEntity {

	@Id
	@Column(name = "id", nullable = false)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "users_id", nullable = false)
	private User user;

	@Column(name = "base_date", nullable = false)
	private LocalDate baseDate;

	@Builder.Default
	@ColumnDefault("'OPEN'")
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private BudgetMonthStatus status = BudgetMonthStatus.OPEN;

	@Builder.Default
	@ColumnDefault("0")
	@Column(name = "total_income_amount", nullable = false)
	private Long totalIncomeAmount = 0L;

	@Builder.Default
	@ColumnDefault("0")
	@Column(name = "total_fixed_essential_amount", nullable = false)
	private Long totalFixedEssentialAmount = 0L;

	@Builder.Default
	@ColumnDefault("0")
	@Column(name = "total_variable_essential_amount", nullable = false)
	private Long totalVariableEssentialAmount = 0L;

	@Column(name = "memo", length = 255)
	private String memo;
}

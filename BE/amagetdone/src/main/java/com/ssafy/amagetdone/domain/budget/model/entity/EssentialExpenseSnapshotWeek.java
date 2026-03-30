package com.ssafy.amagetdone.domain.budget.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.global.entity.BaseEntity;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "essential_expense_snapshot_week")
public class EssentialExpenseSnapshotWeek extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "users_id", nullable = false)
	private User user;

	@Column(name = "year_week", nullable = false, length = 10)
	private String yearWeek;

	@Column(name = "week_start_date", nullable = false)
	private LocalDate weekStartDate;

	@Column(name = "week_end_date", nullable = false)
	private LocalDate weekEndDate;

	@Column(name = "base_months", nullable = false)
	private Integer baseMonths;

	@Builder.Default
	@ColumnDefault("0")
	@Column(name = "essential_amount", nullable = false)
	private Long essentialAmount = 0L;

	@Builder.Default
	@ColumnDefault("0")
	@Column(name = "fixed_essential_amount", nullable = false)
	private Long fixedEssentialAmount = 0L;

	@Builder.Default
	@ColumnDefault("0")
	@Column(name = "variable_essential_amount", nullable = false)
	private Long variableEssentialAmount = 0L;

	@Column(name = "analyzed_at", nullable = false)
	private LocalDateTime analyzedAt;
}

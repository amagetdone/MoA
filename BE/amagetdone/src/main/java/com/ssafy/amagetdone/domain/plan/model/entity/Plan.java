package com.ssafy.amagetdone.domain.plan.model.entity;

import java.time.LocalDate;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;
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
import jakarta.persistence.Table;

@Entity
@Table(name = "plan")
public class Plan extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "users_id", nullable = false)
	private User user;

	@Column(name = "title", nullable = false, length = 100)
	private String title;

	@Column(name = "description", length = 255)
	private String description;

	@Column(name = "target_amount", nullable = false)
	private Long targetAmount;

	@ColumnDefault("0")
	@Column(name = "saved_amount", nullable = false)
	private Long savedAmount = 0L;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "target_weeks", nullable = false)
	private Integer targetWeeks;

	@Column(name = "priority", nullable = false)
	private Integer priority;

	@ColumnDefault("'ACTIVE'")
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private PlanStatus status = PlanStatus.ACTIVE;

	@ColumnDefault("TRUE")
	@Column(name = "auto_allocate_enabled", nullable = false)
	private boolean autoAllocateEnabled = true;

	@Column(name = "plan_img_url", length = 255)
	private String planUrl;

	@Column(name = "plan_video_url", length = 255)
	private String videoUrl;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	protected Plan() {
	}
}

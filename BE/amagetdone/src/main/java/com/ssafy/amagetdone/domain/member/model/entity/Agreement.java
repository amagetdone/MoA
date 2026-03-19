package com.ssafy.amagetdone.domain.member.model.entity;

import java.time.LocalDateTime;

import com.ssafy.amagetdone.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
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
@Table(name = "user_agreements")
public class Agreement extends BaseEntity {

	@Id
	@Column(name = "id", nullable = false)
	private Integer id;

	@Column(name = "agreed", nullable = false)
	private boolean agreed;

	@Column(name = "agreed_at")
	private LocalDateTime agreedAt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "policy_id", nullable = false)
	private Policy policy;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "users_id", nullable = false)
	private User user;
}

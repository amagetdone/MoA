package com.ssafy.amagetdone.domain.member.model.entity;

import com.ssafy.amagetdone.global.entity.BaseEntity;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
@Table(name = "setting")
public class Setting extends BaseEntity {

	@Id
	@Column(name = "id", nullable = false)
	private Integer id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "users_id", nullable = false)
	private User user;

	@ColumnDefault("FALSE")
	@Column(name = "alarm", nullable = false)
	private boolean alarm;
}

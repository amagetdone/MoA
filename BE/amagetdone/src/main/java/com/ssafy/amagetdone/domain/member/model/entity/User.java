package com.ssafy.amagetdone.domain.member.model.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ssafy.amagetdone.global.entity.BaseEntity;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

	@Id
	@Column(name = "id", nullable = false)
	private Integer id;

	@Column(name = "email")
	private String email;

	@Column(name = "nickname", nullable = false, length = 50)
	private String nickname;

	@Column(name = "birth_date")
	private LocalDateTime birthDate;

	@Column(name = "job_status", length = 30)
	private String jobStatus;

	@ColumnDefault("'ACTIVE'")
	@Column(name = "status", nullable = false, length = 20)
	private String status = "ACTIVE";

	@Column(name = "user_api_key", nullable = false, length = 30)
	private String userApiKey;

	@Column(name = "profile_url")
	private String profileUrl;

	@OneToMany(mappedBy = "user")
	private List<SocialAccount> socialAccounts = new ArrayList<>();

	@OneToOne(mappedBy = "user")
	private Setting setting;

	@OneToMany(mappedBy = "user")
	private List<Agreement> agreements = new ArrayList<>();

	protected User() {
	}
}

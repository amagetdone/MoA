package com.ssafy.amagetdone.domain.member.model.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.ssafy.amagetdone.global.entity.BaseEntity;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Integer id;

	@Column(name = "email")
	private String email;

	@Column(name = "nickname", length = 50)
	private String nickname;

	@Column(name = "birth_date")
	private LocalDate birthDate;

	@Column(name = "has_fixed_income")
	private Boolean hasFixedIncome;

	@ColumnDefault("'ACTIVE'")
	@Column(name = "status", nullable = false, length = 20)
	private String status = "ACTIVE";

	@Column(name = "user_api_key", nullable = false, length = 30)
	private String userApiKey;

	@Column(name = "profile_url")
	private String profileUrl;

	@OneToOne(mappedBy = "user")
	private SocialAccount socialAccount;

	@OneToOne(mappedBy = "user")
	private Setting setting;

	@OneToMany(mappedBy = "user")
	private List<Agreement> agreements = new ArrayList<>();

	protected User() {
	}

	public static User createPendingSocialUser(String userApiKey) {
		User user = new User();
		user.userApiKey = userApiKey;
		user.status = "PROFILE_PENDING";
		return user;
	}

	public void completeProfile(String nickname, String email, LocalDate birthDate, Boolean hasFixedIncome) {
		this.nickname = nickname;
		this.email = email;
		this.birthDate = birthDate;
		this.hasFixedIncome = hasFixedIncome;
		this.status = isProfileCompleted() ? "ACTIVE" : "PROFILE_PENDING";
	}

	public boolean isProfileCompleted() {
		return hasText(nickname)
			&& hasText(email)
			&& birthDate != null
			&& hasFixedIncome != null;
	}

	public Integer getId() {
		return id;
	}

	public String getStatus() {
		return status;
	}

	public String getUserApiKey() {
		return userApiKey;
	}

	void linkSocialAccount(SocialAccount socialAccount) {
		this.socialAccount = socialAccount;
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}

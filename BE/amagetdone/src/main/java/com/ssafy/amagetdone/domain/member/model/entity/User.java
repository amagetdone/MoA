package com.ssafy.amagetdone.domain.member.model.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.ssafy.amagetdone.domain.member.model.vo.UserStatus;
import com.ssafy.amagetdone.global.entity.BaseEntity;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private UserStatus status = UserStatus.ACTIVE;

	@Column(name = "user_key", length = 60)
	private String userKey;

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

	public static User createPendingSocialUser() {
		User user = new User();
		user.status = UserStatus.TEMP;
		return user;
	}

	public void markProfileEntered(String nickname, String email, LocalDate birthDate, Boolean hasFixedIncome) {
		this.nickname = nickname;
		this.email = email;
		this.birthDate = birthDate;
		this.hasFixedIncome = hasFixedIncome;
	}

	public void activate() {
		this.status = UserStatus.ACTIVE;
	}

	public void updateUserKey(String userKey) {
		this.userKey = userKey;
	}

	public void updateProfileUrl(String profileUrl) {
		this.profileUrl = profileUrl;
	}

	public void resetToPending() {
		this.email = null;
		this.nickname = null;
		this.birthDate = null;
		this.hasFixedIncome = null;
		this.userKey = null;
		this.profileUrl = null;
		this.status = UserStatus.TEMP;
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

	public String getEmail() {
		return email;
	}

	public String getNickname() {
		return nickname;
	}

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public Boolean getHasFixedIncome() {
		return hasFixedIncome;
	}

	public UserStatus getStatus() {
		return status;
	}

	public String getUserKey() {
		return userKey;
	}

	public String getProfileUrl() {
		return profileUrl;
	}

	void linkSocialAccount(SocialAccount socialAccount) {
		this.socialAccount = socialAccount;
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}

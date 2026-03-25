package com.ssafy.amagetdone.domain.member.support.config;

import java.time.LocalDate;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "test.social-user")
public class TestSocialUserProperties {

	private static final LocalDate DEFAULT_BIRTH_DATE = LocalDate.of(1997, 3, 1);
	private static final boolean DEFAULT_HAS_FIXED_INCOME = true;

	private String nickname;
	private String email;
	private String userApiKey;
	private LocalDate birthDate = DEFAULT_BIRTH_DATE;
	private Boolean hasFixedIncome = DEFAULT_HAS_FIXED_INCOME;

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getUserApiKey() {
		return userApiKey;
	}

	public void setUserApiKey(String userApiKey) {
		this.userApiKey = userApiKey;
	}

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	public Boolean getHasFixedIncome() {
		return hasFixedIncome;
	}

	public void setHasFixedIncome(Boolean hasFixedIncome) {
		this.hasFixedIncome = hasFixedIncome;
	}

	public String resolveNickname(int testUserId) {
		return suffix(testUserId).isEmpty() ? nickname : nickname + testUserId;
	}

	public String resolveEmail(int testUserId) {
		String suffix = suffix(testUserId);
		if (suffix.isEmpty()) {
			return email;
		}

		int separatorIndex = email.indexOf('@');
		if (separatorIndex < 0) {
			return email + suffix;
		}
		return email.substring(0, separatorIndex) + suffix + email.substring(separatorIndex);
	}

	public LocalDate resolveBirthDate(int testUserId) {
		return birthDate == null ? DEFAULT_BIRTH_DATE : birthDate;
	}

	public Boolean resolveHasFixedIncome(int testUserId) {
		return hasFixedIncome == null ? DEFAULT_HAS_FIXED_INCOME : hasFixedIncome;
	}

	private String suffix(int testUserId) {
		return testUserId == 1 ? "" : String.valueOf(testUserId);
	}
}

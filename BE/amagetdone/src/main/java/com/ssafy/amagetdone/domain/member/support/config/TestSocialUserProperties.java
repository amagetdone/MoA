package com.ssafy.amagetdone.domain.member.support.config;

import java.time.LocalDate;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "test.social-user")
public class TestSocialUserProperties {

	private static final LocalDate DEFAULT_BIRTH_DATE = LocalDate.of(1997, 3, 1);
	private static final boolean DEFAULT_HAS_FIXED_INCOME = true;
	private static final String DEFAULT_EMAIL_PREFIX = "moa";
	private static final String DEFAULT_EMAIL_DOMAIN = "tester.com";

	private String nickname;
	private String emailPrefix = DEFAULT_EMAIL_PREFIX;
	private String emailDomain = DEFAULT_EMAIL_DOMAIN;
	private String userApiKey;
	private LocalDate birthDate = DEFAULT_BIRTH_DATE;
	private Boolean hasFixedIncome = DEFAULT_HAS_FIXED_INCOME;

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getEmailPrefix() {
		return emailPrefix;
	}

	public void setEmailPrefix(String emailPrefix) {
		this.emailPrefix = emailPrefix;
	}

	public String getEmailDomain() {
		return emailDomain;
	}

	public void setEmailDomain(String emailDomain) {
		this.emailDomain = emailDomain;
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
		String prefix = isBlank(emailPrefix) ? DEFAULT_EMAIL_PREFIX : emailPrefix;
		String domain = isBlank(emailDomain) ? DEFAULT_EMAIL_DOMAIN : emailDomain;
		return prefix + testUserId + "@" + domain;
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

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}

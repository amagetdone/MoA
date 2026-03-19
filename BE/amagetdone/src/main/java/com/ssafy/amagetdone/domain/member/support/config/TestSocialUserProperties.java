package com.ssafy.amagetdone.domain.member.support.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "test.social-user")
public class TestSocialUserProperties {

	private String nickname;
	private String email;
	private String userApiKey;

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

	private String suffix(int testUserId) {
		return testUserId == 1 ? "" : String.valueOf(testUserId);
	}
}

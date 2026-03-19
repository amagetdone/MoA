package com.ssafy.amagetdone.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "social.auth")
public class SocialAuthProperties {

	private final ProviderProperties google = new ProviderProperties();
	private final ProviderProperties kakao = new ProviderProperties();

	public ProviderProperties getGoogle() {
		return google;
	}

	public ProviderProperties getKakao() {
		return kakao;
	}

	public static class ProviderProperties {

		private String clientId;
		private String userInfoUri;
		private String tokenInfoUri;

		public String getClientId() {
			return clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public String getUserInfoUri() {
			return userInfoUri;
		}

		public void setUserInfoUri(String userInfoUri) {
			this.userInfoUri = userInfoUri;
		}

		public String getTokenInfoUri() {
			return tokenInfoUri;
		}

		public void setTokenInfoUri(String tokenInfoUri) {
			this.tokenInfoUri = tokenInfoUri;
		}
	}
}

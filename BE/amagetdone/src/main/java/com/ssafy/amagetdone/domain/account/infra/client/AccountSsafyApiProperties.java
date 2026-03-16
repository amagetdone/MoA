package com.ssafy.amagetdone.domain.account.infra.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "account.ssafy")
public class AccountSsafyApiProperties {

	private String baseUrl;
	private String institutionCode;
	private String fintechAppNo;

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getInstitutionCode() {
		return institutionCode;
	}

	public void setInstitutionCode(String institutionCode) {
		this.institutionCode = institutionCode;
	}

	public String getFintechAppNo() {
		return fintechAppNo;
	}

	public void setFintechAppNo(String fintechAppNo) {
		this.fintechAppNo = fintechAppNo;
	}
}

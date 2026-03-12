package com.ssafy.amagetdone.domain.transaction.infra.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "transaction.ssafy")
public class TransactionSsafyApiProperties {

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

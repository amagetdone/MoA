package com.ssafy.amagetdone.domain.plan.infra.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pyan")
public class PyanProperties {

	private String baseUrl;
	private String apiKey;
	private String penMode = "no";
	private boolean outputAutocrop = true;
	private long pollInitialDelayMillis = 3000;
	private long pollMaxDelayMillis = 30000;
	private long pollTimeoutMillis = 120000;
	private long readyMissingCompressedInitialDelayMillis = 2000;
	private int readyMissingCompressedMaxAttempts = 3;

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getPenMode() {
		return penMode;
	}

	public void setPenMode(String penMode) {
		this.penMode = penMode;
	}

	public boolean isOutputAutocrop() {
		return outputAutocrop;
	}

	public void setOutputAutocrop(boolean outputAutocrop) {
		this.outputAutocrop = outputAutocrop;
	}

	public long getPollInitialDelayMillis() {
		return pollInitialDelayMillis;
	}

	public void setPollInitialDelayMillis(long pollInitialDelayMillis) {
		this.pollInitialDelayMillis = pollInitialDelayMillis;
	}

	public long getPollMaxDelayMillis() {
		return pollMaxDelayMillis;
	}

	public void setPollMaxDelayMillis(long pollMaxDelayMillis) {
		this.pollMaxDelayMillis = pollMaxDelayMillis;
	}

	public long getPollTimeoutMillis() {
		return pollTimeoutMillis;
	}

	public void setPollTimeoutMillis(long pollTimeoutMillis) {
		this.pollTimeoutMillis = pollTimeoutMillis;
	}

	public long getReadyMissingCompressedInitialDelayMillis() {
		return readyMissingCompressedInitialDelayMillis;
	}

	public void setReadyMissingCompressedInitialDelayMillis(long readyMissingCompressedInitialDelayMillis) {
		this.readyMissingCompressedInitialDelayMillis = readyMissingCompressedInitialDelayMillis;
	}

	public int getReadyMissingCompressedMaxAttempts() {
		return readyMissingCompressedMaxAttempts;
	}

	public void setReadyMissingCompressedMaxAttempts(int readyMissingCompressedMaxAttempts) {
		this.readyMissingCompressedMaxAttempts = readyMissingCompressedMaxAttempts;
	}
}

package com.ssafy.amagetdone.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloud.aws")
public class S3Properties {

	private final Credentials credentials = new Credentials();
	private final Region region = new Region();
	private final S3 s3 = new S3();

	public Credentials getCredentials() {
		return credentials;
	}

	public Region getRegion() {
		return region;
	}

	public S3 getS3() {
		return s3;
	}

	public static class Credentials {

		private String accessKey;
		private String secretKey;

		public String getAccessKey() {
			return accessKey;
		}

		public void setAccessKey(String accessKey) {
			this.accessKey = accessKey;
		}

		public String getSecretKey() {
			return secretKey;
		}

		public void setSecretKey(String secretKey) {
			this.secretKey = secretKey;
		}
	}

	public static class Region {

		private String staticRegion;

		public String getStaticRegion() {
			return staticRegion;
		}

		public void setStaticRegion(String staticRegion) {
			this.staticRegion = staticRegion;
		}
	}

	public static class S3 {

		private String bucket;

		public String getBucket() {
			return bucket;
		}

		public void setBucket(String bucket) {
			this.bucket = bucket;
		}
	}
}

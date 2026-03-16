package com.ssafy.amagetdone.global.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {

	@Bean
	public AmazonS3 amazonS3(S3Properties s3Properties) {
		BasicAWSCredentials credentials = new BasicAWSCredentials(
			s3Properties.getCredentials().getAccessKey(),
			s3Properties.getCredentials().getSecretKey()
		);

		return AmazonS3ClientBuilder.standard()
			.withRegion(s3Properties.getRegion().getStaticRegion())
			.withCredentials(new AWSStaticCredentialsProvider(credentials))
			.build();
	}
}

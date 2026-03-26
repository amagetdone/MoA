package com.ssafy.amagetdone.domain.notification.application.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseConfig {

	private static final String APP_NAME = "amagetdone";

	@Bean
	@ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
	public FirebaseApp firebaseApp(FirebaseProperties properties) throws IOException {
		log.info(
			"Firebase messaging is enabled. projectId={}, credentialsPath={}",
			properties.getProjectId(),
			properties.getCredentialsPath()
		);

		for (FirebaseApp app : FirebaseApp.getApps()) {
			if (APP_NAME.equals(app.getName())) {
				log.info("Reusing existing FirebaseApp '{}'.", APP_NAME);
				return app;
			}
		}

		try (InputStream inputStream = Files.newInputStream(Path.of(properties.getCredentialsPath()))) {
			FirebaseOptions.Builder builder = FirebaseOptions.builder()
				.setCredentials(GoogleCredentials.fromStream(inputStream));
			if (hasText(properties.getProjectId())) {
				builder.setProjectId(properties.getProjectId());
			}
			log.info("Initializing FirebaseApp '{}' for push notifications.", APP_NAME);
			return FirebaseApp.initializeApp(builder.build(), APP_NAME);
		}
	}

	@Bean
	@ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
	public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
		log.info("FirebaseMessaging bean created for app '{}'.", firebaseApp.getName());
		return FirebaseMessaging.getInstance(firebaseApp);
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}

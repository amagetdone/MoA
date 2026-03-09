package com.ssafy.amagetdone;

import java.nio.file.Path;

import com.ssafy.amagetdone.config.EnvLoader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AmagetdoneApplication {

	public static void main(String[] args) {
		EnvLoader.loadRequiredEnv(Path.of("").toAbsolutePath());
		SpringApplication.run(AmagetdoneApplication.class, args);
	}

}

// testtest
package com.ssafy.amagetdone.global.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	@Bean
	public Clock appClock() {
		return Clock.system(KST);
	}
}

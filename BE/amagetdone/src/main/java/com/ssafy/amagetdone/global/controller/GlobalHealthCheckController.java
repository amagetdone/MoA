package com.ssafy.amagetdone.global.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GlobalHealthCheckController {

	@GetMapping("/health")
	public String health() {
		return "Hello World!";
	}
}

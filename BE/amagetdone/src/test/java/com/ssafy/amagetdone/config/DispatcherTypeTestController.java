package com.ssafy.amagetdone.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/test")
public class DispatcherTypeTestController {

	@GetMapping("/dispatcher")
	public String dispatcher() {
		return "ok";
	}
}

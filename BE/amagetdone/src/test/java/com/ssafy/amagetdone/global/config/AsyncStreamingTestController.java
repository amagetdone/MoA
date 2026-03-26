package com.ssafy.amagetdone.global.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/v1/test")
public class AsyncStreamingTestController {

	@GetMapping("/stream")
	StreamingResponseBody stream() {
		return outputStream -> writePayload(outputStream, currentPayload());
	}

	private String currentPayload() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Object principal = authentication == null ? null : authentication.getPrincipal();
		Integer userId = principal instanceof AuthenticatedMember member ? member.userId() : null;
		return "user=" + userId + ",thread=" + Thread.currentThread().getName();
	}

	private void writePayload(java.io.OutputStream outputStream, String payload) throws IOException {
		outputStream.write(payload.getBytes(StandardCharsets.UTF_8));
		outputStream.flush();
	}
}

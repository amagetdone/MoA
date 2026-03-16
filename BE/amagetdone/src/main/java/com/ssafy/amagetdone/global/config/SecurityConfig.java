package com.ssafy.amagetdone.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.global.security.JwtAuthenticationFilter;
import com.ssafy.amagetdone.global.security.SecurityAccessDeniedHandler;
import com.ssafy.amagetdone.global.security.SecurityAuthenticationEntryPoint;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		RequestMatcher publicEndpoints,
		JwtAuthenticationFilter jwtAuthenticationFilter,
		SecurityAuthenticationEntryPoint authenticationEntryPoint,
		SecurityAccessDeniedHandler accessDeniedHandler
	) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler)
			)
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(publicEndpoints).permitAll()
				.anyRequest().authenticated()
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public RequestMatcher publicEndpoints(Environment environment) {
		boolean swaggerPublic = environment.acceptsProfiles(Profiles.of("dev", "test"));
		return request -> {
			String path = normalizePath(request);
			if ("/health".equals(path) || isPublicSwaggerPath(path, swaggerPublic)) {
				return true;
			}

			if (!"POST".equals(request.getMethod())) {
				return false;
			}

			return path.startsWith("/v1/auth/social/")
				|| "/v1/auth/refresh".equals(path)
				|| "/v1/auth/logout".equals(path);
		};
	}

	private boolean isPublicSwaggerPath(String path, boolean swaggerPublic) {
		if (!swaggerPublic) {
			return false;
		}

		return "/swagger-ui.html".equals(path)
			|| path.startsWith("/swagger-ui/")
			|| "/v3/api-docs".equals(path)
			|| path.startsWith("/v3/api-docs/");
	}

	private String normalizePath(jakarta.servlet.http.HttpServletRequest request) {
		String servletPath = request.getServletPath();
		String pathInfo = request.getPathInfo();
		if (servletPath == null || servletPath.isBlank()) {
			return pathInfo == null ? "" : pathInfo;
		}
		if (pathInfo == null || pathInfo.isBlank()) {
			return servletPath;
		}
		return servletPath + pathInfo;
	}

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter(
		RequestMatcher publicEndpoints,
		JwtAccessTokenReader jwtAccessTokenReader,
		SecurityAuthenticationEntryPoint authenticationEntryPoint
	) {
		return new JwtAuthenticationFilter(publicEndpoints, jwtAccessTokenReader, authenticationEntryPoint);
	}

	@Bean
	public SecurityAuthenticationEntryPoint securityAuthenticationEntryPoint(ObjectMapper objectMapper) {
		return new SecurityAuthenticationEntryPoint(objectMapper);
	}

	@Bean
	public SecurityAccessDeniedHandler securityAccessDeniedHandler(ObjectMapper objectMapper) {
		return new SecurityAccessDeniedHandler(objectMapper);
	}
}

package com.ssafy.amagetdone.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.global.security.JwtAuthenticationFilter;
import com.ssafy.amagetdone.global.security.SecurityAccessDeniedHandler;
import com.ssafy.amagetdone.global.security.SecurityAuthenticationEntryPoint;

import java.util.Arrays;
import java.util.List;

import jakarta.servlet.DispatcherType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		CorsConfigurationSource corsConfigurationSource,
		RequestMatcher publicEndpoints,
		JwtAuthenticationFilter jwtAuthenticationFilter,
		SecurityAuthenticationEntryPoint authenticationEntryPoint,
		SecurityAccessDeniedHandler accessDeniedHandler
	) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.cors(cors -> cors.configurationSource(corsConfigurationSource))
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler)
			)
			.authorizeHttpRequests(authorize -> authorize
				.dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
				.requestMatchers(publicEndpoints).permitAll()
				.anyRequest().authenticated()
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(
		@Value("${app.cors.allowed-origins:}") String allowedOriginsValue
	) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowCredentials(true);
		configuration.setAllowedOrigins(parseAllowedOrigins(allowedOriginsValue));
		configuration.setAllowedOriginPatterns(localhostOriginPatterns());
		configuration.setAllowedMethods(List.of(
			HttpMethod.GET.name(),
			HttpMethod.POST.name(),
			HttpMethod.PUT.name(),
			HttpMethod.PATCH.name(),
			HttpMethod.DELETE.name(),
			HttpMethod.OPTIONS.name()
		));
		configuration.setAllowedHeaders(List.of("*"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public RequestMatcher publicEndpoints(Environment environment) {
		return request -> {
			String path = normalizePath(request);
			if ("/health".equals(path) || isPublicSwaggerPath(path)) {
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

	private boolean isPublicSwaggerPath(String path) {
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

	private List<String> parseAllowedOrigins(String allowedOriginsValue) {
		if (allowedOriginsValue == null || allowedOriginsValue.isBlank()) {
			return List.of();
		}

		return Arrays.stream(allowedOriginsValue.split(","))
			.map(String::trim)
			.filter(origin -> !origin.isBlank())
			.toList();
	}

	private List<String> localhostOriginPatterns() {
		return List.of("http://localhost:*", "https://localhost:*");
	}
}

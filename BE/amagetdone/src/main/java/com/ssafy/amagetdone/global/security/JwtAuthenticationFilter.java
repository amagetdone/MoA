package com.ssafy.amagetdone.global.security;

import java.io.IOException;
import java.util.List;

import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final RequestMatcher publicEndpoints;
	private final JwtAccessTokenReader jwtAccessTokenReader;
	private final SecurityAuthenticationEntryPoint authenticationEntryPoint;

	public JwtAuthenticationFilter(
		RequestMatcher publicEndpoints,
		JwtAccessTokenReader jwtAccessTokenReader,
		SecurityAuthenticationEntryPoint authenticationEntryPoint
	) {
		this.publicEndpoints = publicEndpoints;
		this.jwtAccessTokenReader = jwtAccessTokenReader;
		this.authenticationEntryPoint = authenticationEntryPoint;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return publicEndpoints.matches(request);
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		if (SecurityContextHolder.getContext().getAuthentication() != null) {
			filterChain.doFilter(request, response);
			return;
		}

		String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authorizationHeader == null || authorizationHeader.isBlank()) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			int userId = jwtAccessTokenReader.readUserId(authorizationHeader);
			AuthenticatedMember authenticatedMember = new AuthenticatedMember(userId);
			UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
				authenticatedMember,
				null,
				List.of()
			);
			SecurityContextHolder.getContext().setAuthentication(authentication);
			filterChain.doFilter(request, response);
		} catch (MemberException exception) {
			SecurityContextHolder.clearContext();
			authenticationEntryPoint.commence(
				request,
				response,
				new MemberAuthenticationException(exception.getErrorCode(), exception)
			);
		}
	}
}

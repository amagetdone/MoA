package com.ssafy.amagetdone.domain.member.model.repository;

public interface RefreshTokenStore {

	long getTokenVersion(Integer userId);

	void save(Integer userId, String refreshToken, long ttlSeconds);

	boolean exists(Integer userId, String refreshToken);

	void delete(Integer userId, String refreshToken);

	void invalidateAll(Integer userId);
}

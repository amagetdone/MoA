package com.ssafy.amagetdone.domain.member.infra.persistence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import com.ssafy.amagetdone.domain.member.model.repository.RefreshTokenStore;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisRefreshTokenStore implements RefreshTokenStore {

	private final StringRedisTemplate stringRedisTemplate;

	public RedisRefreshTokenStore(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@Override
	public long getTokenVersion(Integer userId) {
		String value = stringRedisTemplate.opsForValue().get(versionKey(userId));
		if (value == null || value.isBlank()) {
			return 0L;
		}
		return Long.parseLong(value);
	}

	@Override
	public void save(Integer userId, String refreshToken, long ttlSeconds) {
		String tokenHash = tokenHash(refreshToken);
		Duration ttl = Duration.ofSeconds(ttlSeconds);
		stringRedisTemplate.opsForValue().set(refreshKey(userId, tokenHash), "1", ttl);
		stringRedisTemplate.opsForSet().add(indexKey(userId), tokenHash);
		stringRedisTemplate.expire(indexKey(userId), ttl);
	}

	@Override
	public boolean exists(Integer userId, String refreshToken) {
		Boolean exists = stringRedisTemplate.hasKey(refreshKey(userId, tokenHash(refreshToken)));
		return Boolean.TRUE.equals(exists);
	}

	@Override
	public void delete(Integer userId, String refreshToken) {
		String tokenHash = tokenHash(refreshToken);
		stringRedisTemplate.delete(refreshKey(userId, tokenHash));
		stringRedisTemplate.opsForSet().remove(indexKey(userId), tokenHash);
	}

	@Override
	public void invalidateAll(Integer userId) {
		Set<String> tokenHashes = stringRedisTemplate.opsForSet().members(indexKey(userId));
		if (tokenHashes != null && !tokenHashes.isEmpty()) {
			List<String> keys = new ArrayList<>();
			for (String tokenHash : tokenHashes) {
				keys.add(refreshKey(userId, tokenHash));
			}
			stringRedisTemplate.delete(keys);
		}
		stringRedisTemplate.delete(indexKey(userId));
		stringRedisTemplate.opsForValue().increment(versionKey(userId));
	}

	private String refreshKey(Integer userId, String tokenHash) {
		return "auth:refresh:" + userId + ":" + tokenHash;
	}

	private String indexKey(Integer userId) {
		return "auth:refresh:index:" + userId;
	}

	private String versionKey(Integer userId) {
		return "auth:token-version:" + userId;
	}

	private String tokenHash(String refreshToken) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			byte[] digest = messageDigest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 algorithm is required", exception);
		}
	}
}

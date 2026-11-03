package com.aetherai.auth.service;

import com.aetherai.common.exception.ApiException;
import com.aetherai.user.entity.UserEntity;
import com.aetherai.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final RefreshTokenRepository tokenRepository;
	private final UserRepository userRepository;
	private final Duration tokenTtl;

	public RefreshTokenService(
			RefreshTokenRepository tokenRepository,
			UserRepository userRepository,
			@Value("${app.auth.refresh-token-ttl:P30D}") Duration tokenTtl) {
		this.tokenRepository = tokenRepository;
		this.userRepository = userRepository;
		this.tokenTtl = tokenTtl;
	}

	@Transactional
	public IssuedRefreshToken issue(UUID userId) {
		return create(userId, UUID.randomUUID(), null);
	}

	@Transactional(noRollbackFor = ApiException.class)
	public RotatedRefreshToken rotate(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw invalidToken();
		}

		RefreshTokenEntity current = tokenRepository.findByTokenHashForUpdate(hash(rawToken))
				.orElseThrow(this::invalidToken);
		Instant now = Instant.now();
		if (current.getRevokedAt() != null) {
			tokenRepository.revokeAllByFamilyId(current.getFamilyId(), now);
			throw invalidToken();
		}
		if (!current.getExpiresAt().isAfter(now)) {
			current.revoke(now, null);
			tokenRepository.save(current);
			throw invalidToken();
		}

		UserEntity user = userRepository.findById(current.getUserId())
				.filter(UserEntity::isEnabled)
				.orElseThrow(this::invalidToken);
		IssuedRefreshToken next = create(user.getId(), current.getFamilyId(), now);
		current.revoke(now, next.id());
		tokenRepository.save(current);
		return new RotatedRefreshToken(user, next);
	}

	@Transactional
	public void revokeAll(UUID userId) {
		tokenRepository.revokeAllByUserId(userId, Instant.now());
	}

	private IssuedRefreshToken create(UUID userId, UUID familyId, Instant now) {
		byte[] random = new byte[48];
		RANDOM.nextBytes(random);
		String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
		UUID tokenId = UUID.randomUUID();
		Instant expiresAt = (now == null ? Instant.now() : now).plus(tokenTtl);
		tokenRepository.save(new RefreshTokenEntity(tokenId, familyId, userId, hash(rawToken), expiresAt));
		return new IssuedRefreshToken(tokenId, rawToken, expiresAt);
	}

	private ApiException invalidToken() {
		return new ApiException(HttpStatus.UNAUTHORIZED, "A sessão expirou. Entre novamente.");
	}

	private String hash(String token) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is required by the runtime.", exception);
		}
	}

	public record IssuedRefreshToken(UUID id, String token, Instant expiresAt) {
	}

	public record RotatedRefreshToken(UserEntity user, IssuedRefreshToken refreshToken) {
	}
}

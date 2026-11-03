package com.aetherai.auth.service;

import com.aetherai.auth.repository.PasswordResetTokenRepository;
import com.aetherai.common.exception.ApiException;
import com.aetherai.user.entity.UserEntity;
import com.aetherai.user.repository.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class PasswordResetService {

	private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
	private static final Duration REQUEST_COOLDOWN = Duration.ofMinutes(1);
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final UserRepository userRepository;
	private final PasswordResetTokenRepository tokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenService refreshTokenService;
	private final ObjectProvider<TransactionalEmailSender> emailSender;
	private final EmailProviderProperties emailProperties;

	public PasswordResetService(
			UserRepository userRepository,
			PasswordResetTokenRepository tokenRepository,
			PasswordEncoder passwordEncoder,
			RefreshTokenService refreshTokenService,
			ObjectProvider<TransactionalEmailSender> emailSender,
			EmailProviderProperties emailProperties) {
		this.userRepository = userRepository;
		this.tokenRepository = tokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.refreshTokenService = refreshTokenService;
		this.emailSender = emailSender;
		this.emailProperties = emailProperties;
	}

	public boolean isConfigured() {
		return emailSender.getIfAvailable() != null;
	}

	@Transactional
	public void requestReset(String email) {
		TransactionalEmailSender sender = emailSender.getIfAvailable();
		if (sender == null) {
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "A recuperação de senha por e-mail ainda não está configurada.");
		}

		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
		UserEntity user = userRepository.findByEmail(normalizedEmail).orElse(null);
		if (user == null) {
			return;
		}

		Instant now = Instant.now();
		if (tokenRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
				.map(PasswordResetTokenEntity::getCreatedAt)
				.filter(createdAt -> createdAt.plus(REQUEST_COOLDOWN).isAfter(now))
				.isPresent()) {
			return;
		}

		byte[] randomToken = new byte[32];
		SECURE_RANDOM.nextBytes(randomToken);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);
		tokenRepository.deleteAllByUserId(user.getId());
		tokenRepository.save(new PasswordResetTokenEntity(
				user.getId(),
				sha256(token),
				now.plus(TOKEN_TTL)));

		String frontendUrl = emailProperties.frontendUrl() == null || emailProperties.frontendUrl().isBlank()
				? "http://localhost:5173"
				: emailProperties.frontendUrl();
		String resetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
				.path("/reset-password")
				.queryParam("token", token)
				.build()
				.encode()
				.toUriString();
		sender.sendPasswordReset(user.getEmail(), resetUrl);
	}

	@Transactional
	public void completeReset(String token, String newPassword) {
		PasswordResetTokenEntity resetToken = tokenRepository
				.findValidTokenForUpdate(sha256(token), Instant.now())
				.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "O link de redefinição é inválido ou expirou."));
		UserEntity user = userRepository.findById(resetToken.getUserId())
				.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "O link de redefinição é inválido ou expirou."));
		user.changePassword(passwordEncoder.encode(newPassword));
		userRepository.save(user);
		refreshTokenService.revokeAll(user.getId());
		resetToken.markUsed(Instant.now());
		tokenRepository.save(resetToken);
		tokenRepository.deleteAllByUserId(user.getId());
	}

	private String sha256(String value) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is required by the runtime.", exception);
		}
	}
}

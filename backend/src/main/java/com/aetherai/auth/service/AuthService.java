package com.aetherai.auth.service;

import com.aetherai.auth.dto.AuthResponse;
import com.aetherai.auth.dto.LoginRequest;
import com.aetherai.auth.dto.RegisterRequest;
import com.aetherai.common.exception.ApiException;
import com.aetherai.user.dto.UserResponse;
import com.aetherai.user.dto.UpdateProfileRequest;
import com.aetherai.user.entity.RoleEntity;
import com.aetherai.user.entity.UserEntity;
import com.aetherai.user.repository.RoleRepository;
import com.aetherai.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtEncoder jwtEncoder;
	private final Duration tokenTtl;
	private final Clock clock;

	public AuthService(
			UserRepository userRepository,
			RoleRepository roleRepository,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			JwtEncoder jwtEncoder,
			@Value("${security.jwt.ttl:PT15M}") Duration tokenTtl) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtEncoder = jwtEncoder;
		this.tokenTtl = tokenTtl;
		this.clock = Clock.systemUTC();
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
		}

		RoleEntity userRole = roleRepository.findByName("USER")
				.orElseThrow(() -> new IllegalStateException("The default USER role is missing."));
		UserEntity user = userRepository.save(new UserEntity(
				email,
				passwordEncoder.encode(request.password()),
				request.displayName().trim(),
				userRole));
		return issueToken(user);
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());
		try {
			authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
		} catch (AuthenticationException exception) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
		}

		UserEntity user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));
		return issueToken(user);
	}

	@Transactional(readOnly = true)
	public UserResponse getCurrentUser(UUID userId) {
		return userRepository.findById(userId)
				.map(UserResponse::from)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
	}

	@Transactional
	public UserResponse updateCurrentUser(UUID userId, UpdateProfileRequest request) {
		UserEntity user = userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
		user.setDisplayName(request.displayName().trim());
		return UserResponse.from(userRepository.save(user));
	}

	private AuthResponse issueToken(UserEntity user) {
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plus(tokenTtl);
		String[] authorities = user.getRoles().stream()
				.map(role -> "ROLE_" + role.getName())
				.toArray(String[]::new);
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("aether-ai")
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.subject(user.getId().toString())
				.claim("email", user.getEmail())
				.claim("roles", authorities)
				.build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(),
				claims)).getTokenValue();

		return new AuthResponse(token, "Bearer", expiresAt, UserResponse.from(user));
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}

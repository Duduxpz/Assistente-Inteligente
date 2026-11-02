package com.aetherai.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aetherai.auth.dto.AuthResponse;
import com.aetherai.auth.dto.LoginRequest;
import com.aetherai.auth.dto.RegisterRequest;
import com.aetherai.common.exception.ApiException;
import com.aetherai.user.dto.UserResponse;
import com.aetherai.user.entity.RoleEntity;
import com.aetherai.user.entity.UserEntity;
import com.aetherai.user.repository.RoleRepository;
import com.aetherai.user.repository.UserRepository;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final AuthenticationManager authenticationManager;
	private final JwtEncoder jwtEncoder;
	private final PasswordEncoder passwordEncoder;
	private final Duration tokenLifetime;

	public AuthService(
			UserRepository userRepository,
			RoleRepository roleRepository,
			AuthenticationManager authenticationManager,
			JwtEncoder jwtEncoder,
			PasswordEncoder passwordEncoder,
			@Value("${security.jwt.ttl}") Duration tokenLifetime) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.authenticationManager = authenticationManager;
		this.jwtEncoder = jwtEncoder;
		this.passwordEncoder = passwordEncoder;
		this.tokenLifetime = tokenLifetime;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new ApiException(HttpStatus.CONFLICT, "Já existe uma conta com este e-mail.");
		}

		RoleEntity userRole = roleRepository.findByName("USER")
				.orElseThrow(() -> new IllegalStateException("Required USER role is missing."));
		UserEntity user = UserEntity.create(
				email,
				passwordEncoder.encode(request.password()),
				request.displayName().strip(),
				userRole);
		return issueToken(userRepository.save(user));
	}

	public AuthResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(email, request.password()));
		UserEntity user = userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalStateException("Authenticated user was not found."));
		return issueToken(user);
	}

	@Transactional(readOnly = true)
	public UserResponse getCurrentUser(UUID userId) {
		UserEntity user = userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "A sessão não é mais válida."));
		return UserResponse.from(user);
	}

	private AuthResponse issueToken(UserEntity user) {
		Instant issuedAt = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("aether-ai")
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(tokenLifetime))
				.subject(user.getId().toString())
				.claim("email", user.getEmail())
				.claim("roles", user.getRoles().stream()
						.map(RoleEntity::getName)
						.sorted()
						.toList())
				.build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(),
				claims)).getTokenValue();
		return new AuthResponse(token, "Bearer", tokenLifetime.toSeconds(), UserResponse.from(user));
	}

	private String normalizeEmail(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}
}

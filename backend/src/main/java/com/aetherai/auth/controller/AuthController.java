package com.aetherai.auth.controller;

import com.aetherai.auth.dto.AuthResponse;
import com.aetherai.auth.dto.LoginRequest;
import com.aetherai.auth.dto.PasswordResetCompletionRequest;
import com.aetherai.auth.dto.PasswordResetRequest;
import com.aetherai.auth.dto.PasswordResetStatusResponse;
import com.aetherai.auth.dto.RegisterRequest;
import com.aetherai.auth.service.AuthService;
import com.aetherai.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private static final String REFRESH_COOKIE = "aether_refresh";

	private final AuthService authService;
	private final PasswordResetService passwordResetService;
	private final boolean secureRefreshCookie;

	public AuthController(
			AuthService authService,
			PasswordResetService passwordResetService,
			@Value("${app.auth.refresh-cookie-secure:false}") boolean secureRefreshCookie) {
		this.authService = authService;
		this.passwordResetService = passwordResetService;
		this.secureRefreshCookie = secureRefreshCookie;
	}

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return sessionResponse(authService.register(request), HttpStatus.CREATED);
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return sessionResponse(authService.login(request), HttpStatus.OK);
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse> refresh(
			@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
		return sessionResponse(authService.refresh(refreshToken), HttpStatus.OK);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
			@AuthenticationPrincipal Jwt jwt) {
		authService.logout(UUID.fromString(jwt.getSubject()));
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
				.build();
	}

	@GetMapping("/password-reset/status")
	public PasswordResetStatusResponse passwordResetStatus() {
		return new PasswordResetStatusResponse(passwordResetService.isConfigured());
	}

	@PostMapping("/password-reset/request")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
		passwordResetService.requestReset(request.email());
	}

	@PostMapping("/password-reset/complete")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void completePasswordReset(@Valid @RequestBody PasswordResetCompletionRequest request) {
		passwordResetService.completeReset(request.token(), request.newPassword());
	}

	private ResponseEntity<AuthResponse> sessionResponse(AuthService.AuthSession session, HttpStatus status) {
		Duration maxAge = Duration.between(java.time.Instant.now(), session.refreshTokenExpiresAt());
		ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, session.refreshToken())
				.httpOnly(true)
				.secure(secureRefreshCookie)
				.sameSite("Strict")
				.path("/api/v1/auth")
				.maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
				.build();
		return ResponseEntity.status(status)
				.header(HttpHeaders.SET_COOKIE, cookie.toString())
				.body(session.response());
	}

	private ResponseCookie expiredRefreshCookie() {
		return ResponseCookie.from(REFRESH_COOKIE, "")
				.httpOnly(true)
				.secure(secureRefreshCookie)
				.sameSite("Strict")
				.path("/api/v1/auth")
				.maxAge(Duration.ZERO)
				.build();
	}
}

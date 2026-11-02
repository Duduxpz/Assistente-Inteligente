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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final PasswordResetService passwordResetService;

	public AuthController(AuthService authService, PasswordResetService passwordResetService) {
		this.authService = authService;
		this.passwordResetService = passwordResetService;
	}

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
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
}

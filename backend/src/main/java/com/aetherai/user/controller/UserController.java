package com.aetherai.user.controller;

import com.aetherai.auth.service.AuthService;
import com.aetherai.user.dto.UserResponse;
import com.aetherai.user.dto.UpdateProfileRequest;
import com.aetherai.user.dto.UserSettingsRequest;
import com.aetherai.user.dto.UserSettingsResponse;
import com.aetherai.user.service.UserSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final AuthService authService;
	private final UserSettingsService settingsService;

	public UserController(AuthService authService, UserSettingsService settingsService) {
		this.authService = authService;
		this.settingsService = settingsService;
	}

	@GetMapping("/me")
	public UserResponse getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
		return authService.getCurrentUser(UUID.fromString(jwt.getSubject()));
	}

	@PutMapping("/me")
	public UserResponse updateProfile(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody UpdateProfileRequest request) {
		return authService.updateCurrentUser(UUID.fromString(jwt.getSubject()), request);
	}

	@GetMapping("/me/settings")
	public UserSettingsResponse getSettings(@AuthenticationPrincipal Jwt jwt) {
		return settingsService.get(UUID.fromString(jwt.getSubject()));
	}

	@PutMapping("/me/settings")
	public UserSettingsResponse updateSettings(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody UserSettingsRequest request) {
		return settingsService.update(UUID.fromString(jwt.getSubject()), request);
	}
}

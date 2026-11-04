package com.aetherai.user.controller;

import com.aetherai.auth.service.AuthService;
import com.aetherai.user.dto.UserResponse;
import com.aetherai.user.dto.UpdateProfileRequest;
import com.aetherai.user.dto.UserSettingsRequest;
import com.aetherai.user.dto.UserSettingsResponse;
import com.aetherai.user.service.UserSettingsService;
import com.aetherai.user.service.UserDataService;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final AuthService authService;
	private final UserSettingsService settingsService;
	private final UserDataService userDataService;

	public UserController(
			AuthService authService,
			UserSettingsService settingsService,
			UserDataService userDataService) {
		this.authService = authService;
		this.settingsService = settingsService;
		this.userDataService = userDataService;
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

	@GetMapping("/me/export")
	public ResponseEntity<StreamingResponseBody> exportData(@AuthenticationPrincipal Jwt jwt) {
		UUID userId = UUID.fromString(jwt.getSubject());
		StreamingResponseBody body = outputStream -> userDataService.writeExport(userId, outputStream);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("application/zip"))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"aether-ai-data.zip\"")
				.body(body);
	}

	@DeleteMapping("/me")
	@ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
	public void deleteAccount(@AuthenticationPrincipal Jwt jwt) {
		userDataService.deleteAccount(UUID.fromString(jwt.getSubject()));
	}
}

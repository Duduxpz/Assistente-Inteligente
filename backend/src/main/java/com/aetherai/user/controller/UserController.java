package com.aetherai.user.controller;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aetherai.auth.service.AuthService;
import com.aetherai.user.dto.UserResponse;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final AuthService authService;

	public UserController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping("/me")
	public UserResponse getCurrentUser(Jwt jwt) {
		return authService.getCurrentUser(UUID.fromString(jwt.getSubject()));
	}
}

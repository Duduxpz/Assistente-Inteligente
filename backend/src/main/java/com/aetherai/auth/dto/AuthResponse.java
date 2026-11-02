package com.aetherai.auth.dto;

import com.aetherai.user.dto.UserResponse;

import java.time.Instant;

public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, UserResponse user) {
}

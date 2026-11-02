package com.aetherai.user.dto;

import java.util.UUID;

import com.aetherai.user.entity.UserEntity;

public record UserResponse(UUID id, String email, String displayName) {

	public static UserResponse from(UserEntity user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName());
	}
}

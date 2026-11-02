package com.aetherai.admin.dto;

import com.aetherai.user.entity.UserEntity;

import java.util.UUID;

public record AdminUserResponse(UUID id, String email, String displayName, boolean enabled) {

	public static AdminUserResponse from(UserEntity user) {
		return new AdminUserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.isEnabled());
	}
}

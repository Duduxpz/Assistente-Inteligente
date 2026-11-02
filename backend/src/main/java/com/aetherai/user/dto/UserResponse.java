package com.aetherai.user.dto;

import com.aetherai.user.entity.UserEntity;

import java.util.List;
import java.util.UUID;

public record UserResponse(UUID id, String email, String displayName, List<String> roles) {

	public static UserResponse from(UserEntity user) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getDisplayName(),
				user.getRoles().stream().map(role -> "ROLE_" + role.getName()).sorted().toList());
	}
}

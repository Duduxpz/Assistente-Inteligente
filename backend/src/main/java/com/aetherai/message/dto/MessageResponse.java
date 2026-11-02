package com.aetherai.message.dto;

import com.aetherai.message.entity.MessageEntity;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(UUID id, String role, String content, Instant createdAt) {

	public static MessageResponse from(MessageEntity message) {
		return new MessageResponse(
				message.getId(),
				message.getRole(),
				message.getContent(),
				message.getCreatedAt());
	}
}

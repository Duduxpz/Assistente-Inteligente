package com.aetherai.conversation.dto;

import com.aetherai.conversation.entity.ConversationEntity;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(UUID id, String title, Instant createdAt, Instant updatedAt) {

	public static ConversationResponse from(ConversationEntity conversation) {
		return new ConversationResponse(
				conversation.getId(),
				conversation.getTitle(),
				conversation.getCreatedAt(),
				conversation.getUpdatedAt());
	}
}

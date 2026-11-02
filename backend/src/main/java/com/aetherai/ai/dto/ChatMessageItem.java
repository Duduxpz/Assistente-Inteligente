package com.aetherai.ai.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageItem(UUID id, String role, String content, Instant createdAt) {
}

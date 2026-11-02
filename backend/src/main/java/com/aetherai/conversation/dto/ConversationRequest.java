package com.aetherai.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConversationRequest(@NotBlank @Size(max = 200) String title) {
}

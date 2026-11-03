package com.aetherai.user.dto;

import com.aetherai.billing.dto.BillingStatusResponse;
import com.aetherai.conversation.dto.ConversationResponse;
import com.aetherai.document.dto.DocumentResponse;
import com.aetherai.message.dto.MessageResponse;
import com.aetherai.task.dto.TaskResponse;
import com.aetherai.usage.dto.UsageSummary;

import java.time.Instant;
import java.util.List;

public record UserDataExport(
		Instant exportedAt,
		UserResponse user,
		UserSettingsResponse settings,
		BillingStatusResponse billing,
		List<TaskResponse> tasks,
		List<ArchivedConversation> conversations,
		List<DocumentResponse> documents,
		UsageSummary usage) {

	public record ArchivedConversation(ConversationResponse conversation, List<MessageResponse> messages) {
	}
}

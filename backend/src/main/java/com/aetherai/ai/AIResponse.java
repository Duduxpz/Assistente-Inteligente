package com.aetherai.ai;

public record AIResponse(String content, Long modelId, Integer inputTokens, Integer outputTokens) {

	public AIResponse {
		if (content == null || content.isBlank()) {
			throw new IllegalArgumentException("AI provider returned an empty response.");
		}
		if ((inputTokens != null && inputTokens < 0) || (outputTokens != null && outputTokens < 0)) {
			throw new IllegalArgumentException("AI token counts cannot be negative.");
		}
	}
}

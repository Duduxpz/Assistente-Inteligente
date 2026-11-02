package com.aetherai.usage.dto;

import com.aetherai.usage.entity.UsageLogEntity;

import java.time.Instant;

public record UsageEntry(
		String operation,
		int inputTokens,
		int outputTokens,
		Instant createdAt) {

	public static UsageEntry from(UsageLogEntity entry) {
		return new UsageEntry(
				entry.getOperation(),
				entry.getInputTokens(),
				entry.getOutputTokens(),
				entry.getCreatedAt());
	}
}

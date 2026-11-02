package com.aetherai.plan.dto;

public record PlanDetails(
		String key,
		String name,
		String description,
		long monthlyPriceCents,
		int monthlyMessages,
		Integer activeTaskLimit,
		Integer conversationLimit,
		long documentStorageBytes) {
}

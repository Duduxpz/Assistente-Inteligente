package com.aetherai.plan;

import java.util.Arrays;

public enum PlanTier {
	FREE("free", "Free", 0, 15, 10, 1, 50L * 1024 * 1024, "Para começar a organizar seu espaço."),
	PLUS("plus", "Plus", 2500, 250, 100, null, 1024L * 1024 * 1024, "Mais espaço para sua rotina e conversas com IA."),
	ULTRA("ultra", "Ultra", 10000, 1000, null, null, 5L * 1024 * 1024 * 1024, "Limites ampliados para usar o AETHER com mais liberdade.");

	private final String key;
	private final String displayName;
	private final long monthlyPriceCents;
	private final int monthlyMessages;
	private final Integer activeTaskLimit;
	private final Integer conversationLimit;
	private final long documentStorageBytes;
	private final String description;

	PlanTier(
			String key,
			String displayName,
			long monthlyPriceCents,
			int monthlyMessages,
			Integer activeTaskLimit,
			Integer conversationLimit,
			long documentStorageBytes,
			String description) {
		this.key = key;
		this.displayName = displayName;
		this.monthlyPriceCents = monthlyPriceCents;
		this.monthlyMessages = monthlyMessages;
		this.activeTaskLimit = activeTaskLimit;
		this.conversationLimit = conversationLimit;
		this.documentStorageBytes = documentStorageBytes;
		this.description = description;
	}

	public String key() {
		return key;
	}

	public String displayName() {
		return displayName;
	}

	public long monthlyPriceCents() {
		return monthlyPriceCents;
	}

	public int monthlyMessages() {
		return monthlyMessages;
	}

	public Integer activeTaskLimit() {
		return activeTaskLimit;
	}

	public Integer conversationLimit() {
		return conversationLimit;
	}

	public long documentStorageBytes() {
		return documentStorageBytes;
	}

	public String description() {
		return description;
	}

	public static PlanTier fromKey(String key) {
		return Arrays.stream(values())
				.filter(plan -> plan.key.equals(key))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Unknown user plan: " + key));
	}
}

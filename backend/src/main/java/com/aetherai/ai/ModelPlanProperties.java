package com.aetherai.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ai.models")
public record ModelPlanProperties(String free, String plus, String ultra) {

	public String forPlan(String planKey) {
		return switch (planKey) {
			case "free" -> free;
			case "plus" -> plus;
			case "ultra" -> ultra;
			default -> throw new IllegalArgumentException("Unknown plan key: " + planKey);
		};
	}
}

package com.aetherai.admin.dto;

import com.aetherai.usage.dto.UsageTotals;

import java.time.Instant;
import java.util.Map;

public record AdminMetricsResponse(
		Instant from,
		Instant to,
		long messages,
		UsageTotals usage,
		Map<String, Long> usersByPlan) {
}

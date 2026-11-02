package com.aetherai.billing.dto;

import java.time.Instant;

public record BillingStatusResponse(
		String planKey,
		String subscriptionStatus,
		Instant currentPeriodEnd,
		boolean cancellationScheduled,
		boolean testModeEnabled,
		boolean testSubscription) {
}

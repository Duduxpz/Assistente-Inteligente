package com.aetherai.billing;

import java.time.Instant;
import java.util.UUID;

public interface MercadoPagoGateway {

	Checkout createSubscription(UUID userId, String email, String planName, long amountCents);

	Subscription getSubscription(String subscriptionId);

	Subscription cancelSubscription(String subscriptionId);

	record Checkout(String subscriptionId, String checkoutUrl) {
	}

	record Subscription(String id, String externalReference, String status, Instant nextPaymentAt) {
	}
}

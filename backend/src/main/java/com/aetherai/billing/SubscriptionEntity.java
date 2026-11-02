package com.aetherai.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
public class SubscriptionEntity {

	@Id
	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "provider_subscription_id", unique = true, length = 80)
	private String providerSubscriptionId;

	@Column(name = "plan_key", nullable = false, length = 16)
	private String planKey;

	@Column(nullable = false, length = 20)
	private String status;

	@Column(name = "checkout_url", length = 2048)
	private String checkoutUrl;

	@Column(name = "current_period_end")
	private Instant currentPeriodEnd;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected SubscriptionEntity() {
	}

	public SubscriptionEntity(UUID userId, String planKey, String providerSubscriptionId, String checkoutUrl) {
		this.userId = userId;
		this.planKey = planKey;
		this.providerSubscriptionId = providerSubscriptionId;
		this.checkoutUrl = checkoutUrl;
		this.status = "pending";
	}

	public SubscriptionEntity(UUID userId, String planKey, Instant currentPeriodEnd) {
		this.userId = userId;
		this.planKey = planKey;
		this.status = "authorized";
		this.currentPeriodEnd = currentPeriodEnd;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getProviderSubscriptionId() {
		return providerSubscriptionId;
	}

	public String getPlanKey() {
		return planKey;
	}

	public String getStatus() {
		return status;
	}

	public String getCheckoutUrl() {
		return checkoutUrl;
	}

	public Instant getCurrentPeriodEnd() {
		return currentPeriodEnd;
	}

	public void updateFromProvider(String status, Instant currentPeriodEnd) {
		this.status = status;
		if (currentPeriodEnd != null) {
			this.currentPeriodEnd = currentPeriodEnd;
		}
		if (!"pending".equals(status)) {
			this.checkoutUrl = null;
		}
	}

	public void activateTestPlan(String planKey, Instant currentPeriodEnd) {
		this.planKey = planKey;
		this.providerSubscriptionId = null;
		this.checkoutUrl = null;
		this.status = "authorized";
		this.currentPeriodEnd = currentPeriodEnd;
	}

	@jakarta.persistence.PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@jakarta.persistence.PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}
}

package com.aetherai.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "webhook_events")
public class WebhookEventEntity {

	@Id
	@Column(name = "event_key", nullable = false, length = 64)
	private String eventKey;

	@Column(name = "request_id", nullable = false, length = 128)
	private String requestId;

	@Column(name = "resource_id", nullable = false, length = 80)
	private String resourceId;

	@Column(name = "processed_at", nullable = false, updatable = false)
	private Instant processedAt;

	protected WebhookEventEntity() {
	}

	public WebhookEventEntity(String eventKey, String requestId, String resourceId) {
		this.eventKey = eventKey;
		this.requestId = requestId;
		this.resourceId = resourceId;
	}

	@jakarta.persistence.PrePersist
	void onCreate() {
		processedAt = Instant.now();
	}
}

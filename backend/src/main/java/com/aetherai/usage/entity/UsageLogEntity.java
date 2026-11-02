package com.aetherai.usage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usage_logs")
public class UsageLogEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "ai_model_id")
	private Long aiModelId;

	@Column(nullable = false, length = 80)
	private String operation;

	@Column(name = "input_tokens", nullable = false)
	private int inputTokens;

	@Column(name = "output_tokens", nullable = false)
	private int outputTokens;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected UsageLogEntity() {
	}

	public UsageLogEntity(
			UUID userId,
			Long aiModelId,
			String operation,
			int inputTokens,
			int outputTokens) {
		this.userId = userId;
		this.aiModelId = aiModelId;
		this.operation = operation;
		this.inputTokens = inputTokens;
		this.outputTokens = outputTokens;
	}

	public Long getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getOperation() {
		return operation;
	}

	public int getInputTokens() {
		return inputTokens;
	}

	public int getOutputTokens() {
		return outputTokens;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	@jakarta.persistence.PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}
}

package com.aetherai.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "ai_models")
public class AIModelEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 80)
	private String provider;

	@Column(name = "model_key", nullable = false, length = 160)
	private String modelKey;

	@Column(name = "display_name", nullable = false, length = 160)
	private String displayName;

	@Column(nullable = false)
	private boolean enabled;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	protected AIModelEntity() {
	}

	public AIModelEntity(String provider, String modelKey, String displayName, boolean enabled) {
		this.provider = provider;
		this.modelKey = modelKey;
		this.displayName = displayName;
		this.enabled = enabled;
	}

	public Long getId() {
		return id;
	}

	public String getProvider() {
		return provider;
	}

	public String getModelKey() {
		return modelKey;
	}

	public String getDisplayName() {
		return displayName;
	}

	public boolean isEnabled() {
		return enabled;
	}
}

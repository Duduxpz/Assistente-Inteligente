package com.aetherai.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_settings")
public class UserSettingsEntity {

	@Id
	@Column(name = "user_id")
	private UUID userId;

	@Column(nullable = false, length = 30)
	private String theme;

	@Column(nullable = false, length = 20)
	private String locale;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected UserSettingsEntity() {
	}

	public UserSettingsEntity(UUID userId, String theme, String locale) {
		this.userId = userId;
		this.theme = theme;
		this.locale = locale;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getTheme() {
		return theme;
	}

	public String getLocale() {
		return locale;
	}

	@jakarta.persistence.PrePersist
	@jakarta.persistence.PreUpdate
	void onSave() {
		updatedAt = Instant.now();
	}
}

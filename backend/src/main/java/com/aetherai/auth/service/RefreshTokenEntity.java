package com.aetherai.auth.service;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

	@Id
	private UUID id;

	@Column(name = "family_id", nullable = false)
	private UUID familyId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "replaced_by")
	private UUID replacedBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected RefreshTokenEntity() {
	}

	public RefreshTokenEntity(
			UUID id,
			UUID familyId,
			UUID userId,
			String tokenHash,
			Instant expiresAt) {
		this.id = id;
		this.familyId = familyId;
		this.userId = userId;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getFamilyId() {
		return familyId;
	}

	public UUID getUserId() {
		return userId;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public void revoke(Instant at, UUID replacementId) {
		revokedAt = at;
		replacedBy = replacementId;
	}

	@jakarta.persistence.PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}
}

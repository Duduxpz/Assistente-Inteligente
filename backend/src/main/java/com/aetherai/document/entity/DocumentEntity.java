package com.aetherai.document.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class DocumentEntity {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "file_name", nullable = false, length = 255)
	private String fileName;

	@Column(name = "content_type", nullable = false, length = 160)
	private String contentType;

	@Column(name = "storage_key", nullable = false, unique = true, length = 512)
	private String storageKey;

	@Column(name = "size_bytes", nullable = false)
	private long sizeBytes;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected DocumentEntity() {
	}

	public DocumentEntity(
			UUID id,
			UUID userId,
			String fileName,
			String contentType,
			String storageKey,
			long sizeBytes) {
		this.id = id;
		this.userId = userId;
		this.fileName = fileName;
		this.contentType = contentType;
		this.storageKey = storageKey;
		this.sizeBytes = sizeBytes;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getFileName() {
		return fileName;
	}

	public String getContentType() {
		return contentType;
	}

	public String getStorageKey() {
		return storageKey;
	}

	public long getSizeBytes() {
		return sizeBytes;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	@jakarta.persistence.PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}
}

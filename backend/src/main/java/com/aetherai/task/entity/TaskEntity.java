package com.aetherai.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class TaskEntity {

	@Id
	@UuidGenerator
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(columnDefinition = "text")
	private String description;

	@Column(nullable = false, length = 20)
	private String priority;

	@Column(nullable = false)
	private boolean completed;

	@Column(name = "due_at")
	private Instant dueAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected TaskEntity() {
	}

	public TaskEntity(UUID userId, String title, String description, String priority, Instant dueAt) {
		this.userId = userId;
		this.title = title;
		this.description = description;
		this.priority = priority;
		this.dueAt = dueAt;
		this.completed = false;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getPriority() {
		return priority;
	}

	public boolean isCompleted() {
		return completed;
	}

	public Instant getDueAt() {
		return dueAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void update(String title, String description, String priority, boolean completed, Instant dueAt) {
		this.title = title;
		this.description = description;
		this.priority = priority;
		this.completed = completed;
		this.dueAt = dueAt;
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

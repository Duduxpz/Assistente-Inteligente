package com.aetherai.message.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class MessageEntity {

	@Id
	@UuidGenerator
	private UUID id;

	@Column(name = "conversation_id", nullable = false)
	private UUID conversationId;

	@Column(name = "model_id")
	private Long modelId;

	@Column(name = "message_role", nullable = false, length = 20)
	private String role;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Column(name = "input_tokens")
	private Integer inputTokens;

	@Column(name = "output_tokens")
	private Integer outputTokens;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected MessageEntity() {
	}

	public MessageEntity(
			UUID conversationId,
			String role,
			String content,
			Long modelId,
			Integer inputTokens,
			Integer outputTokens) {
		this.conversationId = conversationId;
		this.role = role;
		this.content = content;
		this.modelId = modelId;
		this.inputTokens = inputTokens;
		this.outputTokens = outputTokens;
	}

	public UUID getId() {
		return id;
	}

	public UUID getConversationId() {
		return conversationId;
	}

	public String getRole() {
		return role;
	}

	public String getContent() {
		return content;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	@jakarta.persistence.PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}
}

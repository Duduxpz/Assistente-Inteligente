package com.aetherai.task.dto;

import com.aetherai.task.entity.TaskEntity;

import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
		UUID id,
		String title,
		String description,
		String priority,
		boolean completed,
		Instant dueAt,
		Instant createdAt,
		Instant updatedAt) {

	public static TaskResponse from(TaskEntity task) {
		return new TaskResponse(
				task.getId(),
				task.getTitle(),
				task.getDescription(),
				task.getPriority(),
				task.isCompleted(),
				task.getDueAt(),
				task.getCreatedAt(),
				task.getUpdatedAt());
	}
}

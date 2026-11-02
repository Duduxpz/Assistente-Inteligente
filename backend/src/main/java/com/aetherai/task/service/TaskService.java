package com.aetherai.task.service;

import com.aetherai.common.exception.ApiException;
import com.aetherai.task.dto.TaskRequest;
import com.aetherai.task.dto.TaskResponse;
import com.aetherai.task.entity.TaskEntity;
import com.aetherai.task.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TaskService {

	private final TaskRepository taskRepository;

	public TaskService(TaskRepository taskRepository) {
		this.taskRepository = taskRepository;
	}

	@Transactional(readOnly = true)
	public List<TaskResponse> list(UUID userId) {
		return taskRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
				.map(TaskResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public TaskResponse get(UUID userId, UUID taskId) {
		return TaskResponse.from(getOwnedTask(userId, taskId));
	}

	@Transactional
	public TaskResponse create(UUID userId, TaskRequest request) {
		TaskEntity task = new TaskEntity(
				userId,
				request.title().trim(),
				normalizeDescription(request.description()),
				normalizePriority(request.priority()),
				request.dueAt());
		if (request.completed() != null) {
			task.update(task.getTitle(), task.getDescription(), task.getPriority(), request.completed(), task.getDueAt());
		}
		return TaskResponse.from(taskRepository.save(task));
	}

	@Transactional
	public TaskResponse update(UUID userId, UUID taskId, TaskRequest request) {
		TaskEntity task = getOwnedTask(userId, taskId);
		task.update(
				request.title().trim(),
				normalizeDescription(request.description()),
				normalizePriority(request.priority()),
				Boolean.TRUE.equals(request.completed()),
				request.dueAt());
		return TaskResponse.from(taskRepository.save(task));
	}

	@Transactional
	public void delete(UUID userId, UUID taskId) {
		taskRepository.delete(getOwnedTask(userId, taskId));
	}

	private TaskEntity getOwnedTask(UUID userId, UUID taskId) {
		return taskRepository.findByIdAndUserId(taskId, userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found."));
	}

	private String normalizePriority(String priority) {
		return priority == null || priority.isBlank()
				? "medium"
				: priority.toLowerCase(Locale.ROOT);
	}

	private String normalizeDescription(String description) {
		return description == null || description.isBlank() ? null : description.trim();
	}
}

package com.aetherai.task.controller;

import com.aetherai.task.dto.TaskRequest;
import com.aetherai.task.dto.TaskResponse;
import com.aetherai.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

	private final TaskService taskService;

	public TaskController(TaskService taskService) {
		this.taskService = taskService;
	}

	@GetMapping
	public List<TaskResponse> list(@AuthenticationPrincipal Jwt jwt) {
		return taskService.list(userId(jwt));
	}

	@GetMapping("/{taskId}")
	public TaskResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID taskId) {
		return taskService.get(userId(jwt), taskId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TaskResponse create(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody TaskRequest request) {
		return taskService.create(userId(jwt), request);
	}

	@PutMapping("/{taskId}")
	public TaskResponse update(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID taskId,
			@Valid @RequestBody TaskRequest request) {
		return taskService.update(userId(jwt), taskId, request);
	}

	@DeleteMapping("/{taskId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID taskId) {
		taskService.delete(userId(jwt), taskId);
	}

	private UUID userId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}

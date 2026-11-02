package com.aetherai.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record TaskRequest(
		@NotBlank @Size(max = 200) String title,
		@Size(max = 10000) String description,
		@Pattern(regexp = "(?i)low|medium|high", message = "must be low, medium, or high") String priority,
		Boolean completed,
		Instant dueAt) {
}

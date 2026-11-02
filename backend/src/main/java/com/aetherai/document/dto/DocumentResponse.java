package com.aetherai.document.dto;

import com.aetherai.document.entity.DocumentEntity;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
		UUID id,
		String fileName,
		String contentType,
		long sizeBytes,
		Instant createdAt) {

	public static DocumentResponse from(DocumentEntity document) {
		return new DocumentResponse(
				document.getId(),
				document.getFileName(),
				document.getContentType(),
				document.getSizeBytes(),
				document.getCreatedAt());
	}
}

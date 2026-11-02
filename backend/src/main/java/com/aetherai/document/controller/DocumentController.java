package com.aetherai.document.controller;

import com.aetherai.document.dto.DocumentResponse;
import com.aetherai.document.service.DocumentService;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;

@Validated
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

	private final DocumentService documentService;

	public DocumentController(DocumentService documentService) {
		this.documentService = documentService;
	}

	@GetMapping
	public List<DocumentResponse> list(@AuthenticationPrincipal Jwt jwt) {
		return documentService.list(userId(jwt));
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public DocumentResponse upload(
			@AuthenticationPrincipal Jwt jwt,
			@NotNull @RequestPart("file") MultipartFile file) {
		return documentService.upload(userId(jwt), file);
	}

	@GetMapping("/{documentId}/download")
	public ResponseEntity<byte[]> download(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID documentId) {
		DocumentService.DownloadedDocument download = documentService.download(userId(jwt), documentId);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(download.document().getContentType()))
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(download.document().getFileName(), StandardCharsets.UTF_8)
						.build()
						.toString())
				.body(download.content());
	}

	@DeleteMapping("/{documentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID documentId) {
		documentService.delete(userId(jwt), documentId);
	}

	private UUID userId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}

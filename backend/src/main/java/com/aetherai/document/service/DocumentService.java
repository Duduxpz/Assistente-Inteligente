package com.aetherai.document.service;

import com.aetherai.common.exception.ApiException;
import com.aetherai.document.dto.DocumentResponse;
import com.aetherai.document.entity.DocumentEntity;
import com.aetherai.document.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentService.class);
	private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
	private static final Map<String, String> SUPPORTED_TYPES = Map.of(
			".pdf", "application/pdf",
			".txt", "text/plain",
			".md", "text/markdown");

	private final DocumentRepository documentRepository;
	private final Path storageRoot;

	public DocumentService(
			DocumentRepository documentRepository,
			@Value("${app.documents.storage-path}") String storagePath) {
		this.documentRepository = documentRepository;
		this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
	}

	@Transactional(readOnly = true)
	public List<DocumentResponse> list(UUID userId) {
		return documentRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
				.map(DocumentResponse::from)
				.toList();
	}

	@Transactional
	public DocumentResponse upload(UUID userId, MultipartFile file) {
		if (file.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "The uploaded file is empty.");
		}
		if (file.getSize() > MAX_FILE_SIZE) {
			throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Files must be 10 MB or smaller.");
		}

		String fileName = safeFileName(file.getOriginalFilename());
		String extension = extensionOf(fileName);
		String expectedContentType = SUPPORTED_TYPES.get(extension);
		if (expectedContentType == null || !expectedContentType.equalsIgnoreCase(file.getContentType())) {
			throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only PDF, TXT, and Markdown files are supported.");
		}

		UUID documentId = UUID.randomUUID();
		String storageKey = userId + "/" + documentId;
		Path destination = resolveStorageKey(storageKey);
		try {
			Files.createDirectories(destination.getParent());
			try (var input = file.getInputStream()) {
				Files.copy(input, destination);
			}
		} catch (IOException exception) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "The document could not be stored.");
		}

		DocumentEntity document = new DocumentEntity(
				documentId,
				userId,
				fileName,
				expectedContentType,
				storageKey,
				file.getSize());
		try {
			return DocumentResponse.from(documentRepository.save(document));
		} catch (RuntimeException exception) {
			try {
				Files.deleteIfExists(destination);
			} catch (IOException cleanupException) {
				exception.addSuppressed(cleanupException);
				LOGGER.error("Failed to remove a document after its metadata could not be saved.", cleanupException);
			}
			throw exception;
		}
	}

	@Transactional(readOnly = true)
	public DownloadedDocument download(UUID userId, UUID documentId) {
		DocumentEntity document = getOwnedDocument(userId, documentId);
		Path file = resolveStorageKey(document.getStorageKey());
		try {
			if (!Files.isRegularFile(file)) {
				throw new ApiException(HttpStatus.NOT_FOUND, "Document file not found.");
			}
			return new DownloadedDocument(document, Files.readAllBytes(file));
		} catch (IOException exception) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "The document could not be read.");
		}
	}

	@Transactional
	public void delete(UUID userId, UUID documentId) {
		DocumentEntity document = getOwnedDocument(userId, documentId);
		Path file = resolveStorageKey(document.getStorageKey());
		try {
			Files.deleteIfExists(file);
		} catch (IOException exception) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "The document could not be deleted.");
		}
		documentRepository.delete(document);
	}

	private DocumentEntity getOwnedDocument(UUID userId, UUID documentId) {
		return documentRepository.findByIdAndUserId(documentId, userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Document not found."));
	}

	private Path resolveStorageKey(String storageKey) {
		Path file = storageRoot.resolve(storageKey).normalize();
		if (!file.startsWith(storageRoot)) {
			throw new IllegalStateException("Document storage key escapes the configured storage directory.");
		}
		return file;
	}

	private String safeFileName(String originalFileName) {
		if (originalFileName == null || originalFileName.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "A file name is required.");
		}
		String normalized = originalFileName.replace('\\', '/');
		String fileName = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
		if (fileName.isBlank() || fileName.length() > 255 || fileName.chars().anyMatch(Character::isISOControl)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "The file name is invalid.");
		}
		return fileName;
	}

	private String extensionOf(String fileName) {
		int extensionStart = fileName.lastIndexOf('.');
		return extensionStart < 0 ? "" : fileName.substring(extensionStart).toLowerCase(Locale.ROOT);
	}

	public record DownloadedDocument(DocumentEntity document, byte[] content) {
	}
}

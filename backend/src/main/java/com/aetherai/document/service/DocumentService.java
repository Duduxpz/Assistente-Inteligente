package com.aetherai.document.service;

import com.aetherai.common.exception.ApiException;
import com.aetherai.document.dto.DocumentResponse;
import com.aetherai.document.entity.DocumentEntity;
import com.aetherai.document.repository.DocumentRepository;
import com.aetherai.plan.service.PlanLimitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
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
	private final PlanLimitService planLimitService;
	private final Path storageRoot;

	public DocumentService(
			DocumentRepository documentRepository,
			PlanLimitService planLimitService,
			@Value("${app.documents.storage-path}") String storagePath) {
		this.documentRepository = documentRepository;
		this.planLimitService = planLimitService;
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
		planLimitService.ensureCanStoreDocument(userId, file.getSize());

		UUID documentId = UUID.randomUUID();
		String storageKey = userId + "/" + documentId;
		Path destination = resolveStorageKey(storageKey);
		try {
			byte[] content;
			try (var input = file.getInputStream()) {
				content = input.readAllBytes();
			}
			validateContent(extension, content);
			Files.createDirectories(destination.getParent());
			Files.write(destination, content);
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

	private void validateContent(String extension, byte[] content) {
		if (extension.equals(".pdf")) {
			int headerLength = Math.min(content.length, 1024);
			String header = new String(content, 0, headerLength, StandardCharsets.ISO_8859_1);
			if (!header.contains("%PDF-")) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "The file content does not match a PDF document.");
			}
			return;
		}

		try {
			String text = StandardCharsets.UTF_8.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.decode(ByteBuffer.wrap(content))
					.toString();
			if (text.chars().anyMatch(character ->
					Character.isISOControl(character) && character != '\n' && character != '\r' && character != '\t')) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Text files cannot contain binary control characters.");
			}
		} catch (CharacterCodingException exception) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Text files must use valid UTF-8 encoding.");
		}
	}

	public record DownloadedDocument(DocumentEntity document, byte[] content) {
	}
}

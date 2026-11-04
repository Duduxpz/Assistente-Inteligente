package com.aetherai.rag;

import com.aetherai.document.entity.DocumentEntity;
import com.aetherai.common.exception.ApiException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.rag.enabled", havingValue = "true")
public class DocumentIngestionService {

	private final EmbeddingProvider embeddingProvider;
	private final DocumentChunkRepository chunkRepository;
	private final DocumentTextChunker chunker;

	public DocumentIngestionService(
			EmbeddingProvider embeddingProvider,
			DocumentChunkRepository chunkRepository,
			DocumentTextChunker chunker) {
		this.embeddingProvider = embeddingProvider;
		this.chunkRepository = chunkRepository;
		this.chunker = chunker;
	}

	@Transactional
	public void index(DocumentEntity document, byte[] bytes) {
		String text = extract(document, bytes);
		List<String> chunks = chunker.split(text);
		List<DocumentChunkRepository.IndexedChunk> indexed = new ArrayList<>(chunks.size());
		for (int index = 0; index < chunks.size(); index++) {
			String chunk = chunks.get(index);
			indexed.add(new DocumentChunkRepository.IndexedChunk(
					index,
					chunk,
					embeddingProvider.embed(chunk)));
		}
		chunkRepository.replace(document.getUserId(), document.getId(), indexed);
	}

	@Transactional
	public void delete(UUID documentId) {
		chunkRepository.delete(documentId);
	}

	private String extract(DocumentEntity document, byte[] bytes) {
		if ("application/pdf".equals(document.getContentType())) {
			try (PDDocument pdf = Loader.loadPDF(bytes)) {
				return new PDFTextStripper().getText(pdf);
			} catch (IOException exception) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "The PDF could not be parsed for indexing.");
			}
		}
		return new String(bytes, StandardCharsets.UTF_8);
	}
}

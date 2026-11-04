package com.aetherai.rag;

import com.aetherai.plan.service.PlanLimitService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "app.rag.enabled", havingValue = "true")
public class DocumentContextService {

	private final EmbeddingProvider embeddingProvider;
	private final DocumentChunkRepository chunkRepository;
	private final PlanLimitService planLimitService;

	public DocumentContextService(
			EmbeddingProvider embeddingProvider,
			DocumentChunkRepository chunkRepository,
			PlanLimitService planLimitService) {
		this.embeddingProvider = embeddingProvider;
		this.chunkRepository = chunkRepository;
		this.planLimitService = planLimitService;
	}

	public String relevantContext(UUID userId, String query) {
		if (!chunkRepository.hasChunks(userId)) return "";
		int limit = planLimitService.ragChunkLimit(userId);
		var chunks = chunkRepository.search(userId, embeddingProvider.embed(query), limit).stream()
				.filter(chunk -> chunk.similarity() >= 0.1)
				.toList();
		if (chunks.isEmpty()) return "";

		String sources = chunks.stream()
				.map(chunk -> "[Fonte: " + chunk.fileName() + "]\n" + chunk.content())
				.collect(Collectors.joining("\n\n"));
		return sources;
	}
}

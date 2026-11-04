package com.aetherai.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentTextChunker {

	private final int chunkSize;
	private final int overlap;

	public DocumentTextChunker(RagProperties properties) {
		if (properties.chunkSize() < 1
				|| properties.chunkOverlap() < 0
				|| properties.chunkOverlap() >= properties.chunkSize()) {
			throw new IllegalArgumentException("RAG chunk overlap must be non-negative and smaller than chunk size.");
		}
		this.chunkSize = properties.chunkSize();
		this.overlap = properties.chunkOverlap();
	}

	public List<String> split(String source) {
		String text = source == null ? "" : source.replace("\r\n", "\n").replace('\r', '\n').trim();
		if (text.isEmpty()) return List.of();

		List<String> chunks = new ArrayList<>();
		int start = 0;
		while (start < text.length()) {
			int end = Math.min(start + chunkSize, text.length());
			if (end < text.length()) {
				int paragraphBoundary = text.lastIndexOf("\n\n", end);
				int wordBoundary = text.lastIndexOf(' ', end);
				int boundary = Math.max(paragraphBoundary, wordBoundary);
				if (boundary > start + chunkSize / 2) end = boundary;
			}
			String chunk = text.substring(start, end).trim();
			if (!chunk.isEmpty()) chunks.add(chunk);
			if (end == text.length()) break;
			start = Math.max(start + 1, end - overlap);
		}
		return List.copyOf(chunks);
	}
}

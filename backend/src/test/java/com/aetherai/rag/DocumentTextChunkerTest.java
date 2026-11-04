package com.aetherai.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentTextChunkerTest {

	@Test
	void splitsTextWithConfiguredOverlap() {
		DocumentTextChunker chunker = new DocumentTextChunker(new RagProperties(10, 3));

		assertEquals(List.of("ABCDEFGHIJ", "HIJKLMNO"), chunker.split("ABCDEFGHIJKLMNO"));
	}

	@Test
	void rejectsOverlapThatWouldPreventProgress() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new DocumentTextChunker(new RagProperties(10, 10)));
	}
}

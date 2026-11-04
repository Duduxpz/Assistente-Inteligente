package com.aetherai.rag;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "app.rag.enabled", havingValue = "true")
public class DocumentChunkRepository {

	private final JdbcTemplate jdbcTemplate;

	public DocumentChunkRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void replace(UUID userId, UUID documentId, List<IndexedChunk> chunks) {
		delete(documentId);
		for (IndexedChunk chunk : chunks) {
			jdbcTemplate.update("""
					INSERT INTO document_chunks (id, user_id, document_id, chunk_index, content, embedding)
					VALUES (?, ?, ?, ?, ?, CAST(? AS vector))
					""",
					UUID.randomUUID(),
					userId,
					documentId,
					chunk.index(),
					chunk.content(),
					vectorLiteral(chunk.embedding()));
		}
	}

	public void delete(UUID documentId) {
		jdbcTemplate.update("DELETE FROM document_chunks WHERE document_id = ?", documentId);
	}

	public boolean hasChunks(UUID userId) {
		Boolean exists = jdbcTemplate.queryForObject(
				"SELECT EXISTS (SELECT 1 FROM document_chunks WHERE user_id = ?)",
				Boolean.class,
				userId);
		return Boolean.TRUE.equals(exists);
	}

	public List<RelevantChunk> search(UUID userId, float[] queryEmbedding, int limit) {
		String vector = vectorLiteral(queryEmbedding);
		return jdbcTemplate.query("""
				SELECT chunks.content, documents.file_name,
				       1 - (chunks.embedding <=> CAST(? AS vector)) AS similarity
				FROM document_chunks chunks
				JOIN documents ON documents.id = chunks.document_id
				WHERE chunks.user_id = ?
				ORDER BY chunks.embedding <=> CAST(? AS vector)
				LIMIT ?
				""",
				(result, rowNumber) -> new RelevantChunk(
						result.getString("content"),
						result.getString("file_name"),
						result.getDouble("similarity")),
				vector,
				userId,
				vector,
				limit);
	}

	private String vectorLiteral(float[] vector) {
		StringBuilder literal = new StringBuilder("[");
		for (int index = 0; index < vector.length; index++) {
			if (index > 0) literal.append(',');
			literal.append(Float.toString(vector[index]));
		}
		return literal.append(']').toString();
	}

	public record IndexedChunk(int index, String content, float[] embedding) {
	}

	public record RelevantChunk(String content, String fileName, double similarity) {
	}
}

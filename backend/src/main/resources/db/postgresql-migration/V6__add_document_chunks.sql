CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE document_chunks (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    document_id UUID NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536) NOT NULL,
    CONSTRAINT uq_document_chunks_document_index UNIQUE (document_id, chunk_index),
    CONSTRAINT ck_document_chunks_index CHECK (chunk_index >= 0)
);

CREATE INDEX ix_document_chunks_user ON document_chunks (user_id);
CREATE INDEX ix_document_chunks_embedding
    ON document_chunks USING hnsw (embedding vector_cosine_ops);

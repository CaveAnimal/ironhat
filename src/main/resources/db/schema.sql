-- Schema for embedding storage
CREATE TABLE IF NOT EXISTS embeddings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_id VARCHAR(255) NOT NULL,
    chunk_index INT NOT NULL,
    text_content CLOB,
    embedding BLOB,
    created_at TIMESTAMP
);

-- Simple index for doc lookups
CREATE INDEX IF NOT EXISTS idx_embeddings_doc ON embeddings(doc_id);

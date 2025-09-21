-- Flyway V1: create embeddings table with BLOB vector column
CREATE TABLE IF NOT EXISTS embeddings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  content_hash VARCHAR(64) UNIQUE NOT NULL,
  file_path VARCHAR(500) NOT NULL,
  content_type VARCHAR(50) NOT NULL,
  chunk_text CLOB NOT NULL,
  vector BLOB,
  metadata CLOB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_embeddings_file_path ON embeddings(file_path);
CREATE INDEX IF NOT EXISTS idx_embeddings_content_type ON embeddings(content_type);
CREATE INDEX IF NOT EXISTS idx_embeddings_hash ON embeddings(content_hash);

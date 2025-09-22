-- Simple seed for embeddings table used by IndexManager examples
CREATE TABLE IF NOT EXISTS embeddings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  vector BINARY,
  metadata CLOB
);

-- Insert one sample 3-dimensional vector serialized as 3 floats (little-endian)
-- We'll insert the binary using H2 HEX literal for convenience: 3 floats (1.0, 2.0, 3.0)
-- float 1.0 -> 0x0000803F (little endian: 3F800000) but H2 expects hex in big-endian pairs; to be safe, we provide a placeholder and recommend using Java helper if needed.

INSERT INTO embeddings (vector, metadata) VALUES (NULL, '{"dimensions":3, "note":"placeholder — replace with serialized vector bytes for real tests"}');

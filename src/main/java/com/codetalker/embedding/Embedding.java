package com.codetalker.embedding;

import java.time.Instant;

public class Embedding {
    public Long id;
    public String contentHash;
    public String filePath;
    public String contentType;
    public String chunkText;
    public String vectorJson; // JSON array stored as CLOB
    public String metadata;
    public Instant createdAt;
    public Instant updatedAt;

    public Embedding() {}
}

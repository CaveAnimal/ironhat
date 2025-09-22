package com.codetalker.embedding;

import java.util.List;

public interface EmbeddingStore {
    Embedding save(Embedding e) throws Exception;
    Embedding findById(long id) throws Exception;
    Embedding findByHash(String hash) throws Exception;
    List<Embedding> listAll(int limit) throws Exception;
    boolean delete(long id) throws Exception;
}

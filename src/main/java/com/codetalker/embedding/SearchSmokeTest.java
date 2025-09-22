package com.codetalker.embedding;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codetalker.storage.DatabaseManager;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Simple smoke test to exercise embedding generation, persistence and naive search.
 * This uses the existing placeholder EmbeddingService (SHA-256 based) so the
 * numeric similarity values are deterministic but not semantically meaningful.
 */
public class SearchSmokeTest {
    private static final Logger logger = LoggerFactory.getLogger(SearchSmokeTest.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        logger.info("Starting SearchSmokeTest");

        DatabaseManager db = new DatabaseManager("./test-data/smoke-embeddings-" + System.currentTimeMillis());
    com.codetalker.embedding.EmbeddingStore repo = new com.codetalker.embedding.EmbeddingBinaryRepository(db);

        ModelLoader loader = new ModelLoader();
        // try to load default model if present (optional)
        loader.loadModel();
        EmbeddingService embedder = new EmbeddingService(loader, 8);
        VectorSearchService search = new VectorSearchService(repo);

        try {
            // create and store a few embeddings (required DB columns must be set)
            Embedding e1 = new Embedding();
            e1.contentHash = "a";
            e1.filePath = "smoke://alpha";
            e1.contentType = "text/plain";
            e1.chunkText = "alpha";
            float[] v1 = embedder.embed("vec:1,0,0");
            e1.vectorJson = MAPPER.writeValueAsString(asDoubleArray(v1));
            repo.save(e1);

            Embedding e2 = new Embedding();
            e2.contentHash = "b";
            e2.filePath = "smoke://beta";
            e2.contentType = "text/plain";
            e2.chunkText = "beta";
            float[] v2 = embedder.embed("vec:0,1,0");
            e2.vectorJson = MAPPER.writeValueAsString(asDoubleArray(v2));
            repo.save(e2);

            Embedding e3 = new Embedding();
            e3.contentHash = "c";
            e3.filePath = "smoke://gamma";
            e3.contentType = "text/plain";
            e3.chunkText = "gamma";
            float[] v3 = embedder.embed("vec:0,0,1");
            e3.vectorJson = MAPPER.writeValueAsString(asDoubleArray(v3));
            repo.save(e3);

            // Query for something similar to e1
            float[] query = embedder.embed("vec:1,0,0");
            List<VectorSearchService.Hit> results = search.search(query, 3);

            logger.info("Search results:");
            for (int i = 0; i < results.size(); i++) {
                VectorSearchService.Hit r = results.get(i);
                logger.info("#{} -> id={} hash={} score={}", i, r.embedding.id, r.embedding.contentHash, r.score);
            }

        } finally {
            db.shutdown();
            loader.close();
        }

        logger.info("SearchSmokeTest finished");
    }

    private static Double[] asDoubleArray(float[] src) {
        Double[] out = new Double[src.length];
        for (int i = 0; i < src.length; i++) out[i] = Double.valueOf(src[i]);
        return out;
    }
}

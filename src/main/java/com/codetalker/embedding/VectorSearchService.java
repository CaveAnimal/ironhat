package com.codetalker.embedding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Naive vector search: loads embeddings from repository, deserializes JSON vectors,
 * computes cosine similarity with the query vector and returns top-K results.
 */
public class VectorSearchService {
    private final EmbeddingRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();
    private final com.codetalker.ann.ApproxNearestNeighborIndex annIndex; // optional
    private static final Logger logger = LoggerFactory.getLogger(VectorSearchService.class);

    public VectorSearchService(EmbeddingRepository repo) {
        this(repo, null);
    }

    /**
     * Construct the service with an optional ANN index. If an ANN index is provided
     * the existing embeddings will be loaded into it for faster queries.
     */
    public VectorSearchService(EmbeddingRepository repo, com.codetalker.ann.ApproxNearestNeighborIndex annIndex) {
        this.repo = repo;
        this.annIndex = annIndex;
        if (this.annIndex != null) {
            try {
                // Load existing embeddings into the index
                for (Embedding e : repo.listAll(10000)) {
                    float[] vec = parseVector(e.vectorJson);
                    if (vec != null && vec.length > 0) {
                        this.annIndex.add(e.id == null ? e.contentHash : String.valueOf(e.id), vec);
                    }
                }
                this.annIndex.build();
            } catch (Exception ex) {
                // If loading fails, fall back to naive search but keep service usable
                logger.warn("Failed to preload ANN index, continuing without preloaded index: {}", ex.getMessage());
                logger.debug("Detailed preload exception:", ex);
            }
        }
    }

    public static class Hit {
        public final Embedding embedding;
        public final double score;

        public Hit(Embedding e, double score) { this.embedding = e; this.score = score; }
    }

    public List<Hit> search(float[] query, int topK) throws Exception {
        if (annIndex != null) {
            // Use ANN index to retrieve top ids, then fetch full Embedding objects
            String[] ids = annIndex.query(query, topK);
            List<Hit> hits = new ArrayList<>();
            for (String id : ids) {
                Embedding e = null;
                try {
                    e = findByStringId(id);
                } catch (Exception ex) {
                    // lookup error: skip this id but log for visibility
                    logger.warn("Failed to resolve embedding id '{}' from ANN results: {}", id, ex.getMessage());
                    logger.debug("Detailed lookup exception for id {}:", id, ex);
                }
                if (e != null) {
                    float[] vec = parseVector(e.vectorJson);
                    double score = cosineSimilarity(query, vec);
                    hits.add(new Hit(e, score));
                }
            }
            return hits;
        } else {
            List<Embedding> items = repo.listAll(10000); // naive: load up to N entries
            List<Hit> hits = new ArrayList<>();
            for (Embedding e : items) {
                float[] vec = parseVector(e.vectorJson);
                if (vec == null || vec.length == 0) continue;
                double score = cosineSimilarity(query, vec);
                hits.add(new Hit(e, score));
            }
            return hits.stream()
                .sorted(Comparator.comparingDouble((Hit h) -> h.score).reversed()
                    .thenComparing(h -> h.embedding.contentHash == null ? "" : h.embedding.contentHash))
                .limit(topK)
                .collect(Collectors.toList());
        }
    }

    private Embedding findByStringId(String id) throws Exception {
        if (id == null) return null;
        try {
            long lid = Long.parseLong(id);
            return repo.findById(lid);
        } catch (NumberFormatException nfe) {
            return repo.findByHash(id);
        }
    }

    private float[] parseVector(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            List<Double> list = mapper.readValue(json, new TypeReference<List<Double>>() {});
            float[] out = new float[list.size()];
            for (int i = 0; i < list.size(); i++) out[i] = list.get(i).floatValue();
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    private double cosineSimilarity(float[] a, float[] b) {
        int n = Math.min(a.length, b.length);
        double dot = 0.0, na = 0.0, nb = 0.0;
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}

package com.codetalker.ann;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Simple in-memory nearest neighbor index (brute-force) used as a drop-in
 * stand-in until a real ANN library (Jelmerk HNSW) is integrated.
 */
public class InMemoryAnnIndex implements ApproxNearestNeighborIndex {
    private final Map<String, float[]> storage = new ConcurrentHashMap<>();

    @Override
    public void add(String id, float[] vector) {
        storage.put(id, vector);
    }

    @Override
    public void build() {
        // no-op for brute-force
    }

    @Override
    public String[] query(float[] vector, int k) {
        if (vector == null || vector.length == 0) return new String[0];
        List<Map.Entry<String, Float>> scored = new ArrayList<>();
        for (Map.Entry<String, float[]> e : storage.entrySet()) {
            float[] v = e.getValue();
            double score = cosine(vector, v);
            scored.add(Map.entry(e.getKey(), Float.valueOf((float) score)));
        }
        return scored.stream()
            .sorted(Comparator.comparing(Map.Entry<String, Float>::getValue).reversed()
                .thenComparing(Map.Entry::getKey))
            .limit(k)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList())
            .toArray(new String[0]);
    }

    @Override
    public void close() {
        storage.clear();
    }

    private double cosine(float[] a, float[] b) {
        if (a == null || b == null) return 0.0;
        int n = Math.min(a.length, b.length);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}

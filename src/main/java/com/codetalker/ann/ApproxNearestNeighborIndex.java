package com.codetalker.ann;

public interface ApproxNearestNeighborIndex {
    /**
     * Add an item to the index with the provided id and vector.
     */
    void add(String id, float[] vector);

    /**
     * Build/prepare the index for queries. For some implementations this is a no-op.
     */
    void build();

    /**
     * Query the index for top-K nearest ids. Returns an array of ids ordered by
     * descending similarity (best first). Implementations may also provide scores later.
     */
    String[] query(float[] vector, int k);

    /**
     * Close and release any resources.
     */
    void close();
}

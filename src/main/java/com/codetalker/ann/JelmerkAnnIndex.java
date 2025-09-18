package com.codetalker.ann;

import java.util.List;
import java.util.stream.Collectors;

import com.github.jelmerk.hnswlib.core.DistanceFunction;
import com.github.jelmerk.hnswlib.core.DistanceFunctions;
import com.github.jelmerk.hnswlib.core.Item;
import com.github.jelmerk.hnswlib.core.SearchResult;
import com.github.jelmerk.hnswlib.core.hnsw.HnswIndex;

/**
 * Full adapter for the Jelmerk HNSW implementation.
 */
public class JelmerkAnnIndex implements ApproxNearestNeighborIndex {
    private final HnswIndex<String, float[], HnswItem, Float> index;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JelmerkAnnIndex.class);

    private static class HnswItem implements Item<String, float[]> {
        private final String id;
        private final float[] vector;

        HnswItem(String id, float[] vector) {
            this.id = id;
            this.vector = vector;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public float[] vector() {
            return vector;
        }

        @Override
        public int dimensions() {
            return vector != null ? vector.length : 0;
        }
    }

    public JelmerkAnnIndex(int dim) {
        this(dim, 16, 10, 2000);
    }

    /**
     * Create a JelmerkAnnIndex with tunable parameters.
     * @param dim vector dimensionality
     * @param m HNSW parameter m (num links)
     * @param ef query ef (controls recall/latency at query time)
     * @param maxItemCount maximum expected number of items (resized on construction)
     */
    public JelmerkAnnIndex(int dim, int m, int ef, int maxItemCount) {
        DistanceFunction<float[], Float> df = DistanceFunctions.FLOAT_COSINE_DISTANCE;
        HnswIndex.Builder<float[], Float> b = HnswIndex.newBuilder(dim, df, m);
        this.index = b.build();
        try {
            this.index.resize(maxItemCount);
        } catch (Exception ignored) {
        }
        this.index.setEf(ef);
    }

    /**
     * Set ef (query parameter) at runtime to trade recall vs speed.
     */
    public void setEf(int ef) {
        this.index.setEf(ef);
    }

    @Override
    public void add(String id, float[] vector) {
        index.add(new HnswItem(id, vector));
    }

    @Override
    public void build() {
        // HnswIndex builds incrementally on add; nothing special needed
    }

    @Override
    public String[] query(float[] vector, int k) {
        List<SearchResult<HnswItem, Float>> res = index.findNearest(vector, k);
        return res.stream().map(r -> r.item().id()).collect(Collectors.toList()).toArray(new String[0]);
    }

    @Override
    public void close() {
        // no explicit close API
    }

    /**
     * Persist index content (id -> vector) to a file using Java serialization of a Map.
     * This produces a stable, deterministic snapshot that can be loaded back via {@link #loadFrom}.
     */
    public void persistTo(java.nio.file.Path file) throws Exception {
        // Atomic write: write to temp file and move into place
        java.nio.file.Files.createDirectories(file.getParent());
        java.nio.file.Path tmp = java.nio.file.Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
        final byte[] MAGIC = new byte[] { 'I', 'H', 'N', 'S' }; // IronHat N S
        final int VERSION = 1;
        // prepare map of id->vector
        java.util.Map<String, float[]> map = new java.util.HashMap<>();
        for (com.github.jelmerk.hnswlib.core.Item<String, float[]> it : index.items()) {
            map.put(it.id(), java.util.Arrays.copyOf(it.vector(), it.vector().length));
        }
        try (java.io.OutputStream fos = java.nio.file.Files.newOutputStream(tmp, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
            java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(fos);
            java.io.DataOutputStream dos = new java.io.DataOutputStream(bos);
            // header
            dos.write(MAGIC);
            dos.writeInt(VERSION);
            // metadata
            java.util.Map<String,Object> meta = new java.util.HashMap<>();
            meta.put("dimensions", index.getDimensions());
            meta.put("maxItems", index.getMaxItemCount());
            String metaJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(meta);
            byte[] metaBytes = metaJson.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            dos.writeInt(metaBytes.length);
            dos.write(metaBytes);
            // body: serialize map
            java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(dos);
            oos.writeObject(map);
            oos.flush();
            oos.close();
            dos.flush();
        }
        // atomic move
        java.nio.file.Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Load persisted index from a previously-created snapshot file. This will recreate an internal
     * HnswIndex and populate it with the stored vectors.
     */
    @SuppressWarnings("unchecked")
    public void loadFrom(java.nio.file.Path file) throws Exception {
        if (!java.nio.file.Files.exists(file)) throw new java.io.FileNotFoundException(file.toString());
        boolean handled = false;
        // Try header-based format
        try (java.io.InputStream fis = java.nio.file.Files.newInputStream(file)) {
            java.io.DataInputStream dis = new java.io.DataInputStream(new java.io.BufferedInputStream(fis));
            byte[] magic = new byte[4];
            dis.readFully(magic);
            if (magic[0] == 'I' && magic[1] == 'H' && magic[2] == 'N' && magic[3] == 'S') {
                int version = dis.readInt();
                if (version != 1) throw new IllegalStateException("Unsupported persist version: " + version);
                int metaLen = dis.readInt();
                byte[] metaBytes = new byte[metaLen];
                dis.readFully(metaBytes);
                String metaJson = new String(metaBytes, java.nio.charset.StandardCharsets.UTF_8);
                log.info("loadFrom: header metadata={}", metaJson);
                // read remaining ObjectInputStream map
                java.io.ObjectInputStream ois = new java.io.ObjectInputStream(dis);
                Object o = ois.readObject();
                if (o instanceof java.util.Map) {
                    java.util.Map<String, float[]> map = (java.util.Map<String, float[]>) o;
                    if (map.isEmpty()) return;
                    int dim = -1;
                    for (float[] v : map.values()) { if (v != null) { dim = v.length; break; } }
                    if (dim <= 0) return;
                    HnswIndex.Builder<float[], Float> b = HnswIndex.newBuilder(dim, DistanceFunctions.FLOAT_COSINE_DISTANCE, Math.max(map.size(), 16));
                    HnswIndex<String, float[], HnswItem, Float> newIndex = b.build();
                    for (java.util.Map.Entry<String, float[]> e : map.entrySet()) newIndex.add(new HnswItem(e.getKey(), e.getValue()));
                    try { newIndex.setEf(this.index.getEf()); } catch (Exception ignored) {}
                    java.lang.reflect.Field f = this.getClass().getDeclaredField("index");
                    f.setAccessible(true);
                    f.set(this, newIndex);
                    handled = true;
                }
            } else {
                throw new IllegalStateException("no header");
            }
        } catch (Exception headerEx) {
            // header failed, try legacy map-only format
            try (java.io.InputStream fis2 = java.nio.file.Files.newInputStream(file); java.io.ObjectInputStream ois2 = new java.io.ObjectInputStream(new java.io.BufferedInputStream(fis2))) {
                Object o = ois2.readObject();
                if (o instanceof java.util.Map) {
                    java.util.Map<String, float[]> map = (java.util.Map<String, float[]>) o;
                    if (map.isEmpty()) return;
                    int dim = -1;
                    for (float[] v : map.values()) { if (v != null) { dim = v.length; break; } }
                    if (dim <= 0) return;
                    HnswIndex.Builder<float[], Float> b = HnswIndex.newBuilder(dim, DistanceFunctions.FLOAT_COSINE_DISTANCE, Math.max(map.size(), 16));
                    HnswIndex<String, float[], HnswItem, Float> newIndex = b.build();
                    for (java.util.Map.Entry<String, float[]> e : map.entrySet()) newIndex.add(new HnswItem(e.getKey(), e.getValue()));
                    try { newIndex.setEf(this.index.getEf()); } catch (Exception ignored) {}
                    java.lang.reflect.Field f = this.getClass().getDeclaredField("index");
                    f.setAccessible(true);
                    f.set(this, newIndex);
                    handled = true;
                }
            } catch (Exception legacyEx) {
                // final fallback: let Jelmerk load native index file
                HnswIndex<String, float[], HnswItem, Float> loaded = HnswIndex.load(file.toFile());
                java.util.List<com.github.jelmerk.hnswlib.core.Item<String, float[]>> items = new java.util.ArrayList<>();
                for (com.github.jelmerk.hnswlib.core.Item<String, float[]> it : loaded.items()) items.add(it);
                HnswIndex.Builder<float[], Float> b = HnswIndex.newBuilder(loaded.getDimensions(), DistanceFunctions.FLOAT_COSINE_DISTANCE, loaded.getMaxItemCount());
                HnswIndex<String, float[], HnswItem, Float> newIndex = b.build();
                for (com.github.jelmerk.hnswlib.core.Item<String, float[]> it : items) newIndex.add(new HnswItem(it.id(), it.vector()));
                java.lang.reflect.Field f = this.getClass().getDeclaredField("index");
                f.setAccessible(true);
                f.set(this, newIndex);
                handled = true;
            }
        }

        if (!handled) throw new IllegalStateException("Unsupported persisted format for JelmerkAnnIndex");
    }
}


package com.codetalker.ann;

import java.util.Random;

import org.junit.jupiter.api.Test;

public class AnnBenchmarkTest {

    @Test
    public void smallBenchmark() {
        final int n = 1000;
        final int dim = 64;
        final int q = 10;

        Random rnd = new Random(42);

        InMemoryAnnIndex mem = new InMemoryAnnIndex();
        JelmerkAnnIndex jel = new JelmerkAnnIndex(dim);

        for (int i = 0; i < n; i++) {
            float[] v = new float[dim];
            for (int j = 0; j < dim; j++) v[j] = rnd.nextFloat();
            String id = "id-" + i;
            mem.add(id, v);
            jel.add(id, v);
        }
        mem.build();
        jel.build();

        long t0 = System.nanoTime();
        for (int i = 0; i < q; i++) {
            float[] qv = new float[dim];
            for (int j = 0; j < dim; j++) qv[j] = rnd.nextFloat();
            mem.query(qv, 10);
        }
        long memTime = System.nanoTime() - t0;

        t0 = System.nanoTime();
        for (int i = 0; i < q; i++) {
            float[] qv = new float[dim];
            for (int j = 0; j < dim; j++) qv[j] = rnd.nextFloat();
            jel.query(qv, 10);
        }
        long jelTime = System.nanoTime() - t0;

        System.out.println("InMemoryAnn time (ms): " + (memTime / 1_000_000.0));
        System.out.println("JelmerkAnn time (ms): " + (jelTime / 1_000_000.0));

        jel.close();
        mem.close();
    }
}

package com.codetalker.benchmarks;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.codetalker.ann.JelmerkAnnIndex;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class SimpleAnnBenchmark {

    @Param({"64"})
    public int DIM;

    @Param({"5000"})
    public int N;

    @Param({"16"})
    public int M;

    @Param({"200"})
    public int EF;

    private List<float[]> dataset;
    private float[] query;
    private JelmerkAnnIndex jelmerkIndex = null;

    @Setup
    public void setup() {
        Random rnd = new Random(123);
        dataset = new ArrayList<>(N);
        query = new float[DIM];
        for (int i = 0; i < N; i++) {
            float[] v = new float[DIM];
            for (int d = 0; d < DIM; d++) v[d] = rnd.nextFloat();
            dataset.add(v);
        }
        for (int d = 0; d < DIM; d++) query[d] = rnd.nextFloat();
        try {
            // construct Jelmerk index with M, EF and expected maxItemCount
            jelmerkIndex = new JelmerkAnnIndex(DIM, M, EF, Math.max(N, 1024));
            for (int i = 0; i < dataset.size(); i++) jelmerkIndex.add(Integer.toString(i), dataset.get(i));
            jelmerkIndex.build();
        } catch (Throwable t) {
            jelmerkIndex = null;
        }
    }

    @TearDown
    public void tearDown() {
        if (jelmerkIndex != null) {
            try { jelmerkIndex.close(); } catch (Exception ignored) {}
            jelmerkIndex = null;
        }
        dataset = null;
        query = null;
    }

    private static float l2dist(float[] a, float[] b) {
        float s = 0f;
        for (int i = 0; i < a.length; i++) {
            float d = a[i] - b[i];
            s += d * d;
        }
        return s;
    }

    @Benchmark
    public int bruteForceNearest() {
        int best = -1;
        float bestDist = Float.MAX_VALUE;
        for (int i = 0; i < dataset.size(); i++) {
            float dist = l2dist(dataset.get(i), query);
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return best;
    }

    @Benchmark
    public int dummyHnswApprox() {
        if (jelmerkIndex != null) {
            String[] ids = jelmerkIndex.query(query, 1);
            return ids != null && ids.length > 0 ? Integer.parseInt(ids[0]) : -1;
        }
        // fallback: sample-based approximation
        int sample = 50;
        Random rnd = new Random(42);
        int best = -1;
        float bestDist = Float.MAX_VALUE;
        for (int s = 0; s < sample; s++) {
            int i = rnd.nextInt(dataset.size());
            float dist = l2dist(dataset.get(i), query);
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return best;
    }
}

package com.codetalker.benchmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.codetalker.ann.JelmerkAnnIndex;

public class SweepBench {

    public static void main(String[] args) throws Exception {
    // defaults
    int N = 5000;
    int DIM = 64;
    int queries = 200;
    int K = 1; // legacy single-K support
        int[] kList = new int[] {1,5,10};
        int[] mList = new int[] {8, 16, 32};
        int[] efList = new int[] {50, 100, 200};

        // Simple arg parsing: --n --dim --queries --mList comma --efList comma
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--n": N = Integer.parseInt(args[++i]); break;
                case "--dim": DIM = Integer.parseInt(args[++i]); break;
                case "--queries": queries = Integer.parseInt(args[++i]); break;
                case "--k": K = Integer.parseInt(args[++i]); break;
                case "--kList": kList = parseList(args[++i]); break;
                case "--mList": mList = parseList(args[++i]); break;
                case "--efList": efList = parseList(args[++i]); break;
                default: System.err.println("Unknown arg: " + args[i]); break;
            }
        }

    // build header dynamically based on kList
    StringBuilder header = new StringBuilder();
    header.append("# Sweep parameters: N=").append(N).append(" DIM=").append(DIM).append(" queries=").append(queries).append(" kList=");
    for (int ki = 0; ki < kList.length; ki++) { if (ki>0) header.append(","); header.append(kList[ki]); }
    System.out.println(header.toString());
    StringBuilder cols = new StringBuilder();
    cols.append("m,ef");
    for (int ki : kList) cols.append(",recall@"+ki);
    cols.append(",p50ms,p95ms,p99ms,avgQueryMs,buildMs,memoryBytes");
    System.out.println(cols.toString());

        // generate dataset and queries
        Random rnd = new Random(12345);
        List<float[]> dataset = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            float[] v = new float[DIM];
            for (int d = 0; d < DIM; d++) v[d] = rnd.nextFloat();
            dataset.add(v);
        }
        List<float[]> qry = new ArrayList<>(queries);
        for (int i = 0; i < queries; i++) {
            float[] qv = new float[DIM];
            for (int d = 0; d < DIM; d++) qv[d] = rnd.nextFloat();
            qry.add(qv);
        }

        for (int m : mList) {
            for (int ef : efList) {
                // build index
                long buildStart = System.nanoTime();
                JelmerkAnnIndex idx = new JelmerkAnnIndex(DIM, m, ef, Math.max(N, 1024));
                for (int i = 0; i < dataset.size(); i++) idx.add(Integer.toString(i), dataset.get(i));
                idx.build();
                long buildMs = (System.nanoTime() - buildStart) / 1_000_000;

                // measure memory usage after build
                Runtime rt = Runtime.getRuntime();
                rt.gc();
                long usedAfter = rt.totalMemory() - rt.freeMemory();

                // queries
                int maxK = 1;
                for (int ki : kList) if (ki > maxK) maxK = ki;
                long qStart = System.nanoTime();
                int[] matchesPerK = new int[kList.length];
                long[] qDurUs = new long[queries];
                int qi = 0;
                for (float[] qv : qry) {
                    // brute force top-maxK
                    int[] bfTop = bruteForceTopK(dataset, qv, maxK);
                    long beforeQuery = System.nanoTime();
                    String[] ids = idx.query(qv, maxK);
                    long afterQuery = System.nanoTime();
                    // store duration in microseconds for better resolution
                    qDurUs[qi++] = (afterQuery - beforeQuery) / 1_000;

                    // map jel results into int list
                    List<Integer> jelIds = new ArrayList<>();
                    if (ids != null) {
                        for (String id : ids) {
                            try { jelIds.add(Integer.parseInt(id)); } catch (Exception ignored) {}
                        }
                    }

                    // for each k, check intersection with bfTop[0..k-1]
                    for (int ki = 0; ki < kList.length; ki++) {
                        int kk = kList[ki];
                        boolean hit = false;
                        for (int j = 0; j < Math.min(kk, bfTop.length); j++) {
                            for (int jelId : jelIds) { if (jelId == bfTop[j]) { hit = true; break; } }
                            if (hit) break;
                        }
                        if (hit) matchesPerK[ki]++;
                    }
                }
                long totalUs = 0; for (long v: qDurUs) totalUs += v;
                double avgQueryMs = ((double) totalUs / (double) queries) / 1000.0; // convert us->ms
                java.util.Arrays.sort(qDurUs);
                double p50ms = percentile(qDurUs, 50) / 1000.0;
                double p95ms = percentile(qDurUs, 95) / 1000.0;
                double p99ms = percentile(qDurUs, 99) / 1000.0;

                // build output line
                StringBuilder out = new StringBuilder();
                out.append(m).append(',').append(ef);
                for (int ki = 0; ki < kList.length; ki++) {
                    double recallK = (double) matchesPerK[ki] / (double) queries;
                    out.append(',').append(String.format("%.4f", recallK));
                }
                out.append(',').append(String.format("%.6f", p50ms));
                out.append(',').append(String.format("%.6f", p95ms));
                out.append(',').append(String.format("%.6f", p99ms));
                out.append(',').append(String.format("%.6f", avgQueryMs));
                out.append(',').append(buildMs);
                out.append(',').append(usedAfter);
                System.out.println(out.toString());

                try { idx.close(); } catch (Exception ignored) {}
            }
        }
    }

    private static int[] parseList(String s) {
        String[] parts = s.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i].trim());
        return out;
    }

    private static float l2dist(float[] a, float[] b) {
        float s = 0f;
        for (int i = 0; i < a.length; i++) {
            float d = a[i] - b[i];
            s += d * d;
        }
        return s;
    }

    private static int[] bruteForceTopK(List<float[]> dataset, float[] q, int k) {
        int n = dataset.size();
        float[] bestDist = new float[k];
        int[] bestIdx = new int[k];
        for (int i = 0; i < k; i++) { bestDist[i] = Float.MAX_VALUE; bestIdx[i] = -1; }
        for (int i = 0; i < n; i++) {
            float d = l2dist(dataset.get(i), q);
            // insert into top-k if smaller than any
            for (int j = 0; j < k; j++) {
                if (d < bestDist[j]) {
                    // shift right
                    for (int t = k-1; t > j; t--) { bestDist[t] = bestDist[t-1]; bestIdx[t] = bestIdx[t-1]; }
                    bestDist[j] = d; bestIdx[j] = i; break;
                }
            }
        }
        return bestIdx;
    }

    private static double percentile(long[] vals, int p) {
        if (vals == null || vals.length == 0) return 0.0;
        int idx = (int) Math.ceil((p / 100.0) * vals.length) - 1;
        if (idx < 0) idx = 0;
        if (idx >= vals.length) idx = vals.length - 1;
        return (double) vals[idx];
    }
}

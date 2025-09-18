# Benchmarks (quick guide)

This folder contains a small JMH-based benchmarking module and a convenience sweep runner used to evaluate ANN (HNSW) configurations.

Quick actions

- Build the shaded JAR (includes dependencies) used by the PowerShell runner:

  ```powershell
  mvn -f benchmarks -DskipTests package
  ```

  The shaded jar will be created at:

  `benchmarks/target/benchmarks-1.0.0-SNAPSHOT-shaded.jar`

- Run a small grid sweep (PowerShell):

  ```powershell
  powershell -NoProfile -ExecutionPolicy Bypass -File .\benchmarks\run_sweep_grid.ps1 -mList '8,16,32' -efList '50,100,200' -N 200 -DIM 16 -queries 20 -parallel 3 -out live_sweep.csv
  ```

  This launches Java workers (uses the shaded jar inside `benchmarks/target`) in parallel, writes per-job CSVs to `benchmarks/sweep_tmp/` and writes the aggregated CSV to the path you pass to `-out`.

- Test the aggregator without launching Java workers (use existing CSVs in `benchmarks/sweep_tmp/`):

  ```powershell
  powershell -NoProfile -ExecutionPolicy Bypass -File .\benchmarks\run_sweep_grid.ps1 -mList '8,16,32' -efList '50,100,200' -NoRun -out test_synth.csv
  ```

Outputs
- Per-job CSVs: `benchmarks/sweep_tmp/sweep_m<value>_ef<value>.csv` (one file per grid point)
- Aggregated CSV: the `-out` you pass to the script (script writes a comment block that lists source files and detected columns, then a union header and normalized rows)
- Per-job stderr: each per-job CSV has a sibling `.err` file capturing Java stderr; the aggregator includes the first ~20 `.err` lines in the comment block to aid debugging.

Troubleshooting
- If Java fails with "Could not find or load main class ...", ensure you built the shaded jar and that `benchmarks/target/benchmarks-1.0.0-SNAPSHOT-shaded.jar` exists. The runner computes the absolute jar path relative to the script location.
- Increase `-jobTimeoutSeconds` in `run_sweep_grid.ps1` for long-running jobs (default is 600s in the script).
- The aggregator unions `recall@<k>` columns across jobs automatically; if you need a different output format (JSON, gzipped CSV, sorted rows), I can add that.

If you'd like I can add a short example showing how to post-process `live_sweep.csv` into a summary table (by `m` then `ef`) or add a README section with recommended grid-values for different dataset sizes.

Recommended grids
-----------------
These are starter suggestions you can use when sweeping HNSW parameters. Tune to your dataset and constraints.

- Small dataset (N < 10k): `m` = 8,12,16 ; `ef` = 50,100,200
- Medium dataset (10k <= N < 100k): `m` = 12,16,24,32 ; `ef` = 100,200,400
- Large dataset (N >= 100k): `m` = 16,24,32,48 ; `ef` = 200,400,800

Start with a small grid and expand on promising regions. If you want, I can add these as ready-to-run examples in the `run_sweep_grid.ps1` header or provide a JSON grid file loader.


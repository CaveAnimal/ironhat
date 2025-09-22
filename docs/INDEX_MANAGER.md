# IndexManager — Usage & Advanced Tuning

This document shows how to use the `IndexManager` CLI/HTTP rebuild tooling and includes tuning guidance for local development with embedded H2.

## Overview

`IndexManager` rebuilds an ANN index (Jelmerk HNSW) from vectors stored in the `embeddings` table and can persist a snapshot file. It's intended to stream large datasets in batches to avoid OOM on desktop systems.

## Typical CLI usage

Build the project first:

```powershell
mvn -DskipTests=false package
```

Rebuild index and persist to a snapshot file:

```powershell
java -jar target/code-talker-2.0.0-SNAPSHOT.jar index --db jdbc:h2:./data/ironhat-db --out ./target/index-snapshot.bin
```

Start the HTTP server which exposes a `/rebuild` endpoint (default port can be overridden with `--port`):

```powershell
java -jar target/code-talker-2.0.0-SNAPSHOT.jar index --db jdbc:h2:./data/ironhat-db --out ./target/index-snapshot.bin --http --port 8080
```

You can POST to `/rebuild` (no auth by default) to trigger a rebuild. Example using `curl`:

```powershell
curl -X POST http://localhost:8080/rebuild
```

## Batch / fetch tuning

`IndexManager` accepts `--batch-size` and `--fetch-size` to tune DB round-trips and memory usage:

- `--batch-size`: number of vectors fetched and added to the in-memory ANN index per round. Larger values reduce SQL round-trips but raise peak memory usage during indexing. For desktop machines, `512`–`4096` is a reasonable range depending on available RAM.
- `--fetch-size`: JDBC fetch-size hint used when streaming from the DB. Some drivers (Postgres/MySQL) use this to enable server-side cursors/streaming. H2 treats this as a hint. Typical values: `100`–`2000`.

Guidelines:

- If you run out of memory during rebuild, lower `--batch-size` first.
- If you observe many short DB round-trips, increase `--batch-size` to reduce overhead.
- If you have a Postgres server in future, set `--fetch-size` to a few thousand to enable streaming cursors; this is not required for H2.

## HNSW (Jelmerk) tuning

Key HNSW construction parameters you may tune via `IndexManager` flags (if exposed):

- `m`: number of bi-directional links created for each node during indexing. Typical values: `8`–`48` (higher means better recall, slower build and larger index).
- `efConstruction`: controls link selection during construction. Typical values: `100`–`500`.
- `efSearch` (runtime): larger values increase recall at query time at the cost of speed.

For small local indexes (under 100k vectors) defaults are usually fine. For larger datasets increase `m` and `efConstruction` incrementally and measure build time, memory use, and search recall.

## Snapshot persistence

The snapshot file produced via `--out` is a binary representation of the HNSW index. Keep backups and use the persisted snapshot for quick startup without rebuilding from the DB.

## Locking and concurrency

`IndexManager` can optionally take a `--lock-file` path to avoid concurrent rebuilds across processes using a file-based lock. By default in H2-local workflows we rely on file locks only.

## Troubleshooting

- If migrations are not applied, ensure `DatabaseManager` is correctly configured and that the `V1__create_embeddings.sql` migration is present under `src/main/resources/db/migration`.
- If the index build fails due to OOM, reduce `--batch-size` and/or run on a machine with more RAM.

---

If you'd like, I can expand this doc with example memory measurements and a small config table with suggested values per dataset size (e.g., 10k, 100k, 1M vectors).

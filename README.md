# ironhat
Just some stuff I gotta do

## Benchmarks and summarizer notes

## TensorFlow integration (optional)

The project includes an opt-in Maven profile that pulls the (large) TensorFlow Java native artifacts so you can run integration tests that exercise real model-loading code.

- Profile id: `with-tensorflow`
- Activation: use `-Pwith-tensorflow` or pass the property `-DwithTensorFlow=true` on the Maven command line.

Why opt-in?
- The TensorFlow Java distribution is large and platform-specific (native libraries). Keeping it out of the default dependency set keeps developer builds fast and portable; enable it only when you need to run integration tests.

Example commands

Run the regular unit tests (no TF):

```powershell
mvn -DskipTests=false test
```

Run tests with TensorFlow dependencies enabled (may pull large native artifacts):

```powershell
mvn -Pwith-tensorflow -DskipTests=false test
# or equivalently
mvn -DwithTensorFlow=true -DskipTests=false test
```

Notes and platform requirements
- Native libraries: the TensorFlow Java artifacts include platform-specific native libraries. On some platforms you may need additional system libraries or a matching CPU/GPU runtime. If tests fail with UnsatisfiedLinkError or similar, the platform-native libs are likely incompatible or missing.
- Guarded loading: `ModelLoader` attempts to initialize TensorFlow (TFLite `Interpreter` or the full `SavedModelBundle`) reflectively only if TF classes are present. If the TF runtime is not on the classpath the loader will fall back to treating the model file presence as a successful load (or mock mode). This keeps the default test runs lightweight.
- Mock mode for unit tests: to run tests without native model dependencies, you can set the system property `codetalker.model.mock=true`. Some unit tests also set this property internally where appropriate.

Troubleshooting
- If you see reflective initialization warnings during tests (SavedModelBundle or TFLite constructor/method not found), it usually means the TF Java version on the classpath exposes different APIs. We intentionally ignore these warnings and fall back; if you want full TF integration, share the exact TF version/platform you plan to use and I can adapt the reflective loader to that API.

If you want, I can (a) move the TensorFlow dependency fully out of the main `<dependencies>` into the `with-tensorflow` profile (so it's never downloaded for default builds), (b) add a dedicated integration-test profile to run only tests that need the TF runtime, or (c) implement additional reflective signatures for other TF versions. Which would you prefer?

The `benchmarks` scripts produce aggregated sweep CSVs and condensed summaries. When you run the sweep runner with `-AutoSummarize` or call the summarizer directly, it writes `benchmarks/best_table.csv` which contains a compact selection per `m` (rows: `m,ef,recallUsed,comment,avgQueryMs,buildMs,memoryBytes`).

Annotation: when the summarizer cannot find any `ef` value that meets the configured recall threshold for a given `m`, but the `-bestByMaxRecall` flag is enabled, the summarizer will choose the `ef` with the highest recall as a fallback. Those rows will have the `comment` field include the suffix `(max-recall fallback)` so you can tell which selections were chosen by fallback logic.

Example (excerpt from `benchmarks/best_table.csv`):

```
m,ef,recallUsed,comment,avgQueryMs,buildMs,memoryBytes
8,200,0.9400,recall=0.9400 (threshold=0.95) (max-recall fallback),0.396245,1743,6722912
```

This indicates that for `m=8` the `ef=200` row was picked because it had the highest recall available, but its recall (0.94) is below the requested threshold (0.95).

If you prefer the summarizer to fail-to-select instead of using the fallback, omit the `-bestByMaxRecall` flag when invoking the summarizer.

Packaging models in a release
-----------------------------
If you need to intentionally include model artifacts in your packaged jar, enable the explicit `package-models` profile:

```powershell
# Build artifact including models (explicit, opt-in)
mvn -Ppackage-models -DskipTests=false package
```

Use this only when you really intend to distribute models inside the artifact. Default builds exclude `src/main/resources/models/**` to avoid large jars and Windows file-lock issues.

Runtime assertion note (integration tests)
----------------------------------------
The integration test harness now supports opt-in assertions against the embedding shim's `/metrics` endpoint. You can make CI require that the shim has initialized a real runtime by adding system properties to the Maven invocation:

```powershell
# require the shim to have loaded a runtime and expect the sentence-transformers runtime
mvn -DskipTests=false -Dembed.shim.requireLoaded=true -Dembed.shim.expectRuntime=st test
```

These properties are intentionally opt-in so default developer runs remain tolerant of the shim's deterministic fake fallback.

IndexManager (rebuild and snapshot)
----------------------------------
`IndexManager` is a small CLI tool and dev HTTP endpoint to rebuild the HNSW index from persisted embeddings and persist a snapshot to disk.

Usage examples:

Run once from the CLI (detects vector dimension from DB metadata if available):

```powershell
java -cp target/code-talker-2.0.0-SNAPSHOT.jar com.codetalker.embedding.IndexManager --db ./data/mydb --out ./data/index.snap --dim 128
```

Start with an HTTP endpoint to trigger rebuilds remotely (dev only):

```powershell
java -cp target/code-talker-2.0.0-SNAPSHOT.jar com.codetalker.embedding.IndexManager --db ./data/mydb --out ./data/index.snap --http 8111
# then from another shell
Invoke-RestMethod http://localhost:8111/rebuild
```

CLI flags:
- `--db <path>` (required): database file/folder used by `DatabaseManager`.
- `--out <path>` (required): output snapshot path to write the index.
- `--http <port>` (optional): start a small HTTP server exposing `/rebuild` to trigger rebuilds.
- `--dim <n>` (optional): force vector dimensionality (auto-detected from `metadata` if omitted).
- `--m <n>` and `--ef <n>` (optional): HNSW index parameters; defaults are `m=16`, `ef=10`.

Operational notes:
- The HTTP endpoint triggers rebuilds asynchronously and prevents overlapping rebuilds in the same process using a simple in-memory lock. For production or multi-process setups, use an external lock (DB row, filesystem lock, or service coordinator).
- The adapter reads vectors in ID-ordered batches (default batch size 1000) to avoid scanning the entire id range and to limit memory usage. For very large datasets, consider increasing the batch size or implementing more advanced batching/parallel ingestion.
- The snapshot format is deterministic and can be loaded back using `JelmerkAnnIndex.loadFrom` or `JelmerkAnnIndex.loadFrom` via the project's API.


## Local development (H2) & IndexManager

This project uses an embedded H2 database for local development and testing by default. We intentionally avoid Postgres or Docker provisioning for the default desktop-app workflow. If you need Postgres later, request it explicitly and we'll add an optional profile and integration instructions.

Key points:

- Flyway migrations run automatically when the application or tests create a `DataSource` via `DatabaseManager`.
- Embeddings are stored in the `embeddings` table (see `src/main/resources/db/migration/V1__create_embeddings.sql`) with the vector stored as a binary BLOB and metadata as CLOB.

Running the `IndexManager` CLI (rebuild index from DB and persist snapshot):

PowerShell examples (run from repository root):

```
# Build the project
mvn -DskipTests=false package

# Rebuild index and persist to file (example output path)
java -jar target/code-talker-2.0.0-SNAPSHOT.jar index --db jdbc:h2:./data/ironhat-db --out ./target/index-snapshot.bin

# Run IndexManager with HTTP server enabled (listens on configured port, provides /rebuild endpoint)
java -jar target/code-talker-2.0.0-SNAPSHOT.jar index --db jdbc:h2:./data/ironhat-db --out ./target/index-snapshot.bin --http --port 8080

```

Notes:

- The CLI accepts `--batch-size` and `--fetch-size` to tune how many vectors are loaded per DB round-trip; defaults are safe for typical desktop workloads.
- If you pass a `--lock-file` the process will attempt a file-based lock to avoid concurrent rebuilds across processes. Postgres advisory locks are not required for local H2 development and are intentionally not used by default.


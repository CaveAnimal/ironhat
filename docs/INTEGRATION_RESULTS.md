## Integration Test Results (summary)

Date: 2025-09-19

Location of artifacts: `target/failsafe-reports/`

Key artifacts collected:

- `failsafe-summary.xml` — summary: 6 completed, 0 errors, 0 failures, 1 skipped.
- `EmbedServiceIntegrationTestIT-shim.log` — shim stdout/stderr captured while integration tests attempted to start a local embedding shim. Contains Uvicorn startup lines, two successful `/embed` POSTs when a shim was available, and multiple attempts that failed to bind port 8000 due to address-in-use errors on Windows (winerror 10048).
- `com.codetalker.embedding.ModelLoaderEnvVarIntegrationTest.txt` — environment-var based ModelLoader test ran successfully after adding `--add-opens` to test JVM argLine.
- `com.codetalker.embedding.TFSavedModelIntegrationTest.txt` — skipped (no local SavedModel provided).

Observations & notes:

- The integration run completed successfully after fixes to the test JVM arguments (`--add-opens`) and making the EmbedService integration test robust to port collisions. See `ironhat_POM_UPDATES.md` for a recorded POM change.
- The shim log shows the embedding shim started and handled POSTs when available; other attempts failed because port 8000 was already in use on the host. The test harness was updated to pick an ephemeral free port when starting the shim to avoid this race.
- SavedModel-based tests were intentionally skipped when no TF SavedModel path was provided; provide a `-Dcodetalker.model.path` pointing to a SavedModel or TFLite file to exercise those tests.

Action items:

- If you want SavedModel/SavedModel-based integration coverage, provision a local SavedModel or TFLite and rerun `mvn -Pwith-tensorflow -Dcodetalker.model.path=<path> verify`.
- Consider enabling `ci-no-shim` profile in CI if a remote shim is provided by CI orchestration.

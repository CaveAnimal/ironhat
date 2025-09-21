# ironhat_POM_UPDATES

This file records every change made to `pom.xml` in the `FeH-001` branch and the reason for each change. The goal is transparency: every modification must be logged here with a timestamp, author (assistant or user), the exact diff summary, and rationale.

## Entries

- 2025-09-17T10:41:00Z — assistant
  - Change: Replaced unresolved `org.bytedeco:tensorflow-lite:2.19.0-1.5.12` dependency with official `org.tensorflow:tensorflow-lite:2.13.0` and `org.tensorflow:tensorflow-lite-select-jni:2.13.0` artifacts.
  - Rationale: Attempt to use official TensorFlow Lite artifacts and ensure platform JNI binaries are selected via `tensorflow-lite-select-jni`. This was an automated attempt to resolve dependency resolution failures for TFLite.
  - Outcome: User later reversed this change; record kept for audit.

- 2025-09-17T10:10:00Z — assistant
  - Change: Previously toggled (commented) official `org.tensorflow:tensorflow-lite` dependency and added `org.bytedeco:tensorflow-lite` as a temporary alternative.
  - Rationale: Earlier attempts to enable TF Lite caused Maven resolution failures; Bytedeco attempted as alternative.
  - Outcome: Caused further resolution issues; user reverted.

- 2025-09-17T11:10:00Z — assistant (pending)
  - Change: Will add official `org.tensorflow:tensorflow-lite:2.13.0` and `org.tensorflow:tensorflow-lite-select-jni:2.13.0` dependencies to `pom.xml`, and remove/comment the `org.bytedeco` entry.
  - Rationale: Force POM to reference official artifacts so we can attempt a fresh dependency resolution from Google Maven. This entry will be updated with the actual outcome after the build attempt.
  - Outcome: (to be filled after build)
  
  - ActualChangeTimestamp: 2025-09-17T11:13:00Z
  - ChangeAppliedBy: assistant
  - DiffSummary: Uncommented/added `org.tensorflow:tensorflow-lite:2.13.0` and `org.tensorflow:tensorflow-lite-select-jni:2.13.0`; commented out `org.bytedeco:tensorflow-lite`.
  - Reason: Attempt to resolve official TF artifacts from Google Maven and ensure runtime JNI selection.
  - Next: Running forced build to attempt resolution; outcome will be appended below.
  - Outcome: BUILD FAILURE when attempting to resolve official TensorFlow artifacts.
  - ErrorSummary: Could not find artifacts `org.tensorflow:tensorflow-lite:2.13.0`, `org.tensorflow:tensorflow-lite-api:2.13.0`, or `org.tensorflow:tensorflow-lite-select-jni:2.13.0` in configured repositories (central or google-maven).
  - Notes: Maven attempted to download POM/JARs from both central and Google Maven but they are not present at the expected coordinates. The `tensorflow-lite-select-jni` POM was reported missing.
  - SuggestedNext: Consider using Bytedeco or a different coordinate/version, or obtain platform-specific TFLite JNI artifacts from an alternative repository or manual install.

- 2025-09-17T11:36:29Z — assistant
  - Change: Attempted to fetch `org.tensorflow:tensorflow-lite:2.17.0` (transitive) via `mvn dependency:get`.
  - Rationale: Try a newer version (2.17.0) that might be published where older 2.13.0 wasn't.
  - Outcome: Failed. Maven returned a plugin resolution error and could not locate the artifact in configured repositories.
  - ErrorSummary: "Error resolving version for plugin '.tensorflow:tensorflow-lite' ... Plugin not found in any plugin repository" — suggests artifact not available at those coordinates or Maven misinterpreting the coordinate resolution.
  - Next: Recommend reverting POM to the previously working dependency (Bytedeco) or attempt manual/local installation of the JNI artifact. Alternatively, I can try to discover available `org.tensorflow` versions programmatically.

- 2025-09-17T11:45:00Z — assistant
  - Change: Updated `pom.xml` to use `org.tensorflow:tensorflow-lite:2.17.0` and `org.tensorflow:tensorflow-lite-api:2.17.0`; omitted `tensorflow-lite-select-jni` because that artifact was not present on Maven Central.
  - Rationale: Maven Central contains `tensorflow-lite` and `tensorflow-lite-api` for 2.17.0 but `tensorflow-lite-select-jni` was not available; adding the available artifacts lets compile against the API and core library. Native JNI binaries will need a separate approach.
  - Next: Running `mvn -U -DskipTests=false clean test` to attempt resolution and run tests; outcome will be appended.

- 2025-09-17T11:55:00Z — assistant
  - Change: Replaced TensorFlow TFLite dependencies in `pom.xml` with `org.tensorflow:tensorflow-core-platform:0.4.0` (full TensorFlow Java distribution).
  - Rationale: Attempt to bring in the full TensorFlow Java runtime (including native libs) in a single dependency to avoid fragmented artifacts and relocations.
  - Next: Running `mvn -U -DskipTests=false clean test` to resolve and run tests; outcome will be appended.
 
- 2025-09-19T14:35:00-05:00 — assistant
  - Change: Added `argLine` JVM option `--add-opens java.base/java.lang=ALL-UNNAMED` to both the `maven-surefire-plugin` and `maven-failsafe-plugin` configurations in `pom.xml` so tests that reflectively access `java.lang.ProcessEnvironment` can run under Java 9+ strong encapsulation.
  - Change: Improved `EmbedServiceIntegrationTestIT` to probe `127.0.0.1:8000` first and otherwise start the Python embedding shim on an ephemeral free TCP port; shim logs are now written with a port-specific filename to avoid collisions.
  - Rationale: Fix `InaccessibleObjectException` seen when running tests on Java 21 and make the embedding-shim startup robust to port collisions on developer machines and CI.
  - Outcome: Ran `mvn -Pwith-tensorflow -Dcodetalker.model.path=src/main/resources/models/universal-sentence-encoder-lite.tflite verify` — tests passed (BUILD SUCCESS). See target/failsafe-reports for detailed outputs.
  - Next: Consider adding CI-level guidance for running with `-Pci-no-shim` when an external shim is provided, and document the test harness behavior in `docs/TESTING.md`.

- 2025-09-19T14:40:00-05:00 — assistant
  - Change: Recorded integration artifacts and summary in `docs/INTEGRATION_RESULTS.md`; shim logs indicating intermittent port-bind failures (winerror 10048) were captured and addressed in the test harness.
  - Rationale: Keep an auditable summary of CI/integration runs and the reasoning for recent test harness changes.
  - Outcome: `docs/INTEGRATION_RESULTS.md` added with pointers to `target/failsafe-reports` and recommended next steps.




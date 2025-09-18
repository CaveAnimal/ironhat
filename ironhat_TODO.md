<!-- ironhat_TODO.md - Assistant-maintained todo list for project planning and actions -->
# Assistant TODOs (tracked by GitHub Copilot assistant)

This file records the assistant's working todo items, their status, and short notes. The assistant will update this file as tasks are started, completed, or deferred so the project owner can see planned work.

## Current Assistant Plan

- [x] Create `ironhat_TODO.md` and append reference to `TheRules.md` — Created and reference appended.
- [x] Add reflection fallback unit test (`TFEmbeddingServiceReflectionFallbackTest`) — Completed
- [x] Add invocation-time exception test (`TFEmbeddingServiceInvocationExceptionTest`) — Completed
- [x] Run full unit tests after changes — Completed (BUILD SUCCESS)
- [x] Add large-embedding smoke test (`TFEmbeddingServiceLargeEmbeddingSmokeTest`) — Completed
- [ ] Scaffold Maven project (`pom.xml`, basic src layout) — Not started
- [ ] Add `ModelLoader`, `DatabaseManager`, `EmbeddingService` skeletons from examples — Not started
- [ ] Add unit test harness and CI-friendly test runner — Not started

## Notes
- The assistant will mirror this file with the structured todo list used internally (see project `manage_todo_list`).

Last updated: 2025-09-17

## Assistant Internal Todo Snapshot

- In-progress: Switch to tensorflow-core-platform and build (completed)
- Completed: Reproduce/collect failing tests; Analyze surefire reports; Check pom and build with tests; Apply official TF to POM and run build; Try alternate TF coordinates/versions
- Next: Fix failing tests; Run full test suite and validate

## ANN Integration Plan

- Preferred starting library: **Jelmerk (hnswlib-core)**. We will create a small abstraction `ApproxNearestNeighborIndex` and an in-memory implementation for tests. The project now includes a `JelmerkAnnIndex` adapter and tests. We will continue by tuning parameters, adding persistence, and integrating the adapter into the search service.

Next steps:

- [ ] Tune `JelmerkAnnIndex` parameters (m, ef, efConstruction, maxItemCount) and add integration tests
- [ ] Add persistence (save/load) support and round-trip tests
- [ ] Add benchmarking (JMH) and performance regression checks


Timestamp: 2025-09-17T12:05:00-05:00

## Recent assistant actions (appended)

- [2025-09-18] Added unit tests for reflection fallback and invocation-time exceptions; verified full unit test suite passes (BUILD SUCCESS). — Assistant

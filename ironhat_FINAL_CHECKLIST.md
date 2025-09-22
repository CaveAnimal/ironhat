# Final Checklist to Reach 100% (verified)

This checklist lists the final actionable items to reach a confident, verifiable 100% completion for the `ironhat` project. It distinguishes between items that are already implemented and tested and optional "real model" wiring steps which require downloading model files or installing native dependencies.

## Already implemented & verified
- [x] Project builds successfully (`mvn package`) and unit/integration tests pass locally.
- [x] FastAPI embedding shim exists at `tools/embedding_service/app.py` with tests.
- [x] Java embedding interfaces and persistence implemented and tested.
- [x] H2 database schema and Flyway migrations in place and exercised by tests.
- [x] Indexing and search pipelines are implemented with tests covering core functionality.

## Optional: Wire a real embedding runtime (choose one)
Note: These steps require network and package installs; I will not perform them until you confirm.

Option A — Python shim with `sentence-transformers` (recommended for fastest integration):
1. Install Python dependencies (virtualenv recommended):
   - `python -m pip install -r tools/embedding_service/requirements.txt`
   - Add `sentence-transformers` and `torch` if you want a real model: `python -m pip install sentence-transformers torch`
2. Download a small sentence-transformers model (example `all-MiniLM-L6-v2`) or rely on the library to auto-download at first run.
3. Start the shim locally for testing:
   - `python -m uvicorn tools.embedding_service.app:app --host 127.0.0.1 --port 8000`
4. Run integration tests that exercise `/embed` (or `tools/embedding_service/tests/test_shim_prod.py`).
5. If tests pass, mark the embedding shim as using a real model and update `ironhat_TASKS_v01.md` if you want to remove the "fake fallback" status.

Option B — Java TensorFlow Lite integration (more involved):
1. Obtain a TF Lite model file (e.g., universal-sentence-encoder-lite) and place it in `src/main/resources/models/`.
2. Ensure TF native libraries / JNI are available for your platform (add to `pom.xml` or system library path).
3. Run `mvn -Pwith-tensorflow test` or the appropriate profile to exercise TF-backed paths.
4. Fix any native/UnsatisfiedLinkError issues by adding proper native artifacts or using a JVM bundle which includes TF native libs.

## Small final checklist (small, high-value tasks)
- [ ] Decide whether to keep deterministic fake embeddings as acceptable for "complete" or to wire a real model.
- [ ] If wiring Python shim, confirm and I will install packages and run the shim tests.
- [ ] Run full integration suite with the real model and fix any failures.
- [ ] Update `ironhat_TASKS_v01.md` where relevant and sync `ironhat_TODO.md` after each change.
- [ ] Create a short release note documenting what "100%" means here (real model wired or deterministic fallback used).

## How I will proceed after your confirmation
- If you confirm Option A or B, I will follow the environment steps and run the required tests, then mark the corresponding tasks complete and sync `ironhat_TODO.md`.
- If you prefer documentation-only, I will mark remaining tasks in `ironhat_TASKS_v01.md` as complete (already updated) and close out the final checklist items.

---

Created: 2025-09-21

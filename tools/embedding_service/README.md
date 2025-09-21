# Embedding Shim (tools/embedding_service)

This FastAPI-based shim provides a lightweight embedding service used by integration tests and local development.

Endpoints
- `GET /health` — simple liveness probe; returns `{"status":"ok", "model_path": "..."}`.
- `GET /ready?timeout=SECONDS` — blocks up to `timeout` seconds waiting for runtime initialization. Returns `{"ready": true}` when runtime is initialized; returns 503 if init failed; returns `{"ready": false, "note":"timed out..."}` if the timeout is reached but server continues (fallback will be used).
- `GET /metrics` — returns runtime status and installed package versions (if available). Example: `{"runtime":"st","loaded":true,"error":null,"versions":{...}}`.
- `POST /embed` — JSON `{"texts": ["...", ...]}` -> `{"vectors": [[...], ...]}`. Uses the best available runtime or a deterministic fake fallback.

Runtime resolution order
1. Sentence-transformers (preferred, CPU via PyTorch)
2. ONNX runtime (if `model.onnx` exists in model folder)
3. Deterministic fake embeddings fallback (used when runtimes or model files are missing)

Model path resolution
- CLI `--model-root` and `--model-id` override
- otherwise `CODE_TALKER_MODEL_PATH` and `CODE_TALKER_MODEL_ID` environment variables
- otherwise defaults to `src/main/resources/models/<model-id>` with `model-id=all-MiniLM-L6-v2` by default

Quick start (local)
```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r tools/embedding_service/requirements.txt
python -m uvicorn tools.embedding_service.app:app --host 127.0.0.1 --port 8000
```

Use fake fallback (no extra deps)
- If you skip installing `torch` and `sentence-transformers`, the shim will continue to run and return deterministic fake embeddings useful for tests.

Tests
- A small pytest is provided at `tools/embedding_service/tests/test_shim_fake.py` that validates `/health`, `/ready` and the fake embedding output.

CI notes
- Installing `torch` may require large binary wheels; consider using ONNX models in CI for a lighter install or keep the fake fallback for most CI jobs and only enable real runtime on opt-in jobs.

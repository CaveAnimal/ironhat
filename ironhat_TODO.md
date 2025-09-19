```markdown
<!-- ironhat_TODO.md - Assistant-maintained todo list for project planning and actions -->
# Assistant TODOs (tracked by GitHub Copilot assistant)

This file records the assistant's working todo items, their status, and short notes. The assistant will update this file as tasks are started, completed, or deferred so the project owner can see planned work.

## Current Assistant Plan

- [x] Run TF integration tests (prepare) -- Prepare to run Maven integration profile `with-tensorflow`; ensure a TensorFlow SavedModel or .tflite path and confirm TF native libraries are available on the machine.
- [x] Execute TF integration tests -- Attempted to run `mvn -Pwith-tensorflow -Dcodetalker.model.path=<SAVED_MODEL_PATH> verify` using local `universal-sentence-encoder-lite.tflite` in `src/main/resources/models`; Maven failed due to PowerShell argument parsing error. Capture diagnostic output and prepare re-run.
- [ ] Collect integration artifacts and update docs -- If integration tests pass, summarize results and update POM/docs with TF notes and model path.
- [-] Retry TF integration with correct quoting and native checks -- Re-run Maven with a PowerShell-safe quoted `-Dcodetalker.model.path` and, if the run starts, watch for native TensorFlow/TFLite errors (UnsatisfiedLinkError).
- [x] Download HF model all-MiniLM-L6-v2 -- Use `huggingface_hub.snapshot_download` to download `sentence-transformers/all-MiniLM-L6-v2` into `src/main/resources/models/all-MiniLM-L6-v2`. Honor `HF_TOKEN` if set in the environment.
- [-] Mirror todo rule enforcement -- Ensure that when the assistant adds/updates the internal `manage_todo_list`, it immediately writes the same changes to `ironhat_TODO.md` per project rule.
- [x] Move runtime-essential files to flat model path -- Copy config/tokenizer/model files from the HF snapshot into `src/main/resources/models/all-MiniLM-L6-v2/` for predictable Java/REST loading.
- [-] Add Python FastAPI shim (embed endpoint) -- Create a FastAPI app that loads the model from `src/main/resources/models/all-MiniLM-L6-v2` and exposes `/embed` POST to return embeddings.
- [ ] Add Java client snippet -- Add a Java example using `HttpClient` showing how to call the shim and parse float[][] vectors.
- [x] Add embedding usage README -- Document flatten step, how to run FastAPI shim, and Java client usage in `docs/EMBEDDING_README.md`.

Last synced: 2025-09-19 02:08:54 PM CDT

```
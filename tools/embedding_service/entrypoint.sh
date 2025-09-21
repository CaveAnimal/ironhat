#!/usr/bin/env bash
# Entrypoint for the embedding shim container
set -euo pipefail
if [ -n "${MODEL_ROOT:-}" ]; then
  export CODE_TALKER_MODEL_PATH="$MODEL_ROOT"
fi
exec uvicorn app:app --host 0.0.0.0 --port ${PORT:-8000}

#!/usr/bin/env python3
"""Embedding shim used by integration tests.

Behavior:
- Attempts to load a real embedding model using sentence-transformers (preferred) or ONNX as a fallback.
- If the runtime or model files are not available, falls back to a deterministic fake-embedding generator so
  tests remain fast and hermetic.

CLI / env var model resolution order:
- --model-root (CLI) -> CODE_TALKER_MODEL_PATH (env) -> default src/main/resources/models
- --model-id (CLI) -> CODE_TALKER_MODEL_ID (env) -> default all-MiniLM-L6-v2

This file exposes:
  GET /health
  POST /embed  (JSON {"texts": [...]})
"""
from __future__ import annotations

import argparse
import math
import os
from pathlib import Path
from typing import List, Optional, Tuple

from fastapi import FastAPI, HTTPException, Response
from pydantic import BaseModel

import logging
import numpy as np
try:
    from prometheus_client import CollectorRegistry, generate_latest, CONTENT_TYPE_LATEST, Counter, Gauge
    _PROM_AVAILABLE = True
except Exception:
    _PROM_AVAILABLE = False


app = FastAPI()
logger = logging.getLogger("embedding_shim")
logging.basicConfig(level=logging.INFO, format="[%(asctime)s] %(levelname)s %(name)s: %(message)s")

# Prometheus metrics (optional; if prometheus_client is not installed the import will fail at startup)
if _PROM_AVAILABLE:
    registry = CollectorRegistry()
    METRIC_REQUESTS = Counter("embed_requests_total", "Total embed requests", registry=registry)
    METRIC_RUNTIME_LOADED = Gauge("embed_runtime_loaded", "Runtime loaded (1=yes,0=no)", registry=registry)
else:
    registry = None
    METRIC_REQUESTS = None
    METRIC_RUNTIME_LOADED = None

DEFAULT_MODEL_ROOT = os.path.join("src", "main", "resources", "models")
DEFAULT_MODEL_ID = "all-MiniLM-L6-v2"


class EmbedRequest(BaseModel):
    texts: List[str]


# Globals for lazy-loaded runtimes
_st_model = None
_onnx_session = None
_onnx_tokenizer = None
_model_path: Path = Path(DEFAULT_MODEL_ROOT) / DEFAULT_MODEL_ID


def resolve_model_path(cli_root: Optional[str], cli_id: Optional[str]) -> Path:
    root = cli_root or os.getenv("CODE_TALKER_MODEL_PATH") or DEFAULT_MODEL_ROOT
    mid = cli_id or os.getenv("CODE_TALKER_MODEL_ID") or DEFAULT_MODEL_ID
    return Path(root) / mid


def _fake_embed_texts(texts: List[str], dim: int = 384) -> List[List[float]]:
    out = []
    for t in texts:
        h = 1469598103934665603  # FNV offset basis
        for ch in t:
            h ^= ord(ch)
            h *= 1099511628211
            h &= (1 << 64) - 1
        vec = []
        seed = h
        for i in range(dim):
            seed = (seed * 6364136223846793005 + 1442695040888963407) & ((1 << 64) - 1)
            val = math.sin((seed & 0xFFFFFFFF) / 4294967295.0 + i * 1.61803398875)
            vec.append(val)
        out.append(vec)
    return out


def try_load_sentence_transformer(model_path: Path):
    """Try to load a SentenceTransformer model from model_path.

    Returns the model instance or raises ImportError/RuntimeError on failure.
    """
    try:
        from sentence_transformers import SentenceTransformer
    except Exception as e:
        raise ImportError("sentence-transformers not available") from e

    # SentenceTransformer will accept either a local path or a HF id string.
    if model_path.exists():
        # load from local path
        model = SentenceTransformer(str(model_path))
    else:
        # attempt to load by id (may require network)
        model = SentenceTransformer(str(model_path))
    return model


def try_load_onnx(model_path: Path) -> Tuple[Optional[object], Optional[object]]:
    """Try to load an ONNX session and tokenizer from model_path.

    Returns (session, tokenizer) or (None, None) on failure.
    """
    try:
        import onnxruntime as ort
        from transformers import AutoTokenizer
    except Exception:
        return None, None

    model_onnx = model_path / "model.onnx"
    if not model_onnx.exists():
        return None, None

    session = ort.InferenceSession(str(model_onnx), providers=["CPUExecutionProvider"])
    tokenizer = AutoTokenizer.from_pretrained(str(model_path))
    return session, tokenizer


def load_runtime(model_path: Path):
    """Load the best available runtime.

    Priority: sentence-transformers -> ONNX -> None (fallback to fake embeddings)
    """
    global _st_model, _onnx_session, _onnx_tokenizer
    if _st_model is not None:
        return "st", _st_model

    # Try sentence-transformers (PyTorch-backed, CPU)
    try:
        _st_model = try_load_sentence_transformer(model_path)
        print(f"[embedding_shim] loaded sentence-transformers model from {model_path}")
        return "st", _st_model
    except Exception as e:
        print(f"[embedding_shim] sentence-transformers not available or failed: {e}")

    # Try ONNX
    sess, tok = try_load_onnx(model_path)
    if sess is not None:
        _onnx_session = sess
        _onnx_tokenizer = tok
        print(f"[embedding_shim] loaded ONNX model from {model_path}")
        return "onnx", (_onnx_session, _onnx_tokenizer)

    print("[embedding_shim] no runtime available, using fake embeddings")
    return None, None


# Readiness tracking
_runtime_loaded = False
_runtime_error: Optional[str] = None


def _background_load():
    global _runtime_loaded, _runtime_error
    try:
        logger.info(f"background runtime init starting for {_model_path}")
        rt, _ = load_runtime(_model_path)
        _runtime_loaded = True
        logger.info(f"background runtime init finished: {rt}")
    except Exception as e:
        _runtime_error = str(e)
        _runtime_loaded = False
        logger.exception("background runtime init failed")


@app.get("/health")
def health():
    try:
        path = str(_model_path)
    except Exception:
        path = "<unknown>"
    return {"status": "ok", "model_path": path}


@app.get("/metrics")
def prometheus_metrics():
    try:
        METRIC_RUNTIME_LOADED.set(1 if _runtime_loaded else 0)
        data = generate_latest(registry)
        return Response(content=data, media_type=CONTENT_TYPE_LATEST)
    except Exception as e:
        # Fallback to simple metrics JSON when prometheus_client isn't available
        return {"runtime": "st" if _st_model is not None else ("onnx" if _onnx_session is not None else None),
                "loaded": _runtime_loaded}


@app.get("/ready")
def ready(timeout: int = 30):
    """Block until the runtime is initialized or timeout (seconds) reached.

    Returns 200 once runtime is available (or fake fallback selected). If runtime init
    fails and no fallback is available, returns 503.
    """
    import time

    # If already initialized, return quickly
    if _runtime_loaded:
        return {"ready": True}

    # If an error was recorded previously, return 503
    if _runtime_error is not None:
        raise HTTPException(status_code=503, detail=f"runtime error: {_runtime_error}")

    # Otherwise poll until timeout
    start = time.time()
    while time.time() - start < timeout:
        if _runtime_loaded:
            return {"ready": True}
        if _runtime_error is not None:
            raise HTTPException(status_code=503, detail=f"runtime error: {_runtime_error}")
        time.sleep(0.5)

    # timed out; still return 200 but indicate not-ready (server is up and will use fallback)
    return {"ready": False, "note": "timed out waiting for runtime; fallback may be used"}


@app.get("/metrics")
@app.on_event("startup")
def _on_startup():
    global _model_path, _runtime_loaded, _runtime_error
    # Resolve model path from env/args (if provided by CLI wrapper)
    _model_path = resolve_model_path(None, None)
    logger.info(f"startup: resolved model_path={_model_path}")
    try:
        # Kick off background initialization in a thread so startup is non-blocking
        import threading

        t = threading.Thread(target=_background_load, daemon=True)
        t.start()
    except Exception:
        logger.exception("failed to start background loader")


@app.on_event("shutdown")
def _on_shutdown():
    logger.info("shutting down embedding shim")



def mean_pooling_from_onnx(outputs, attention_mask: np.ndarray) -> np.ndarray:
    # outputs is a list from session.run; assume first is last_hidden_state
    token_embeddings = outputs[0]
    input_mask_expanded = attention_mask[:, :, None].astype(np.float32)
    sum_embeddings = (token_embeddings * input_mask_expanded).sum(axis=1)
    sum_mask = input_mask_expanded.sum(axis=1)
    sum_mask = np.where(sum_mask == 0, 1e-9, sum_mask)
    return sum_embeddings / sum_mask


@app.post("/embed")
def embed(req: EmbedRequest):
    if not req.texts:
        raise HTTPException(status_code=400, detail="No texts provided")

    # Ensure model path resolved
    global _model_path
    # load runtime lazily
    runtime, model = load_runtime(_model_path)
    texts = req.texts
    if runtime == "st":
        # sentence-transformers: model.encode -> numpy
        vecs = model.encode(texts, convert_to_numpy=True)
        return {"vectors": vecs.tolist()}
    elif runtime == "onnx":
        session, tokenizer = model
        # tokenize using tokenizer; expect numpy arrays
        encoded = tokenizer(texts, padding=True, truncation=True, return_tensors="np")
        ort_inputs = {k: v for k, v in encoded.items()}
        outputs = session.run(None, ort_inputs)
        vectors = mean_pooling_from_onnx(outputs, encoded["attention_mask"])
        return {"vectors": vectors.tolist()}
    else:
        # fallback fake embeddings
        vecs = _fake_embed_texts(texts)
        return {"vectors": vecs}


def parse_cli_args():
    parser = argparse.ArgumentParser(description="Embedding shim")
    parser.add_argument("--model-root", dest="model_root", help="Path to model root (overrides CODE_TALKER_MODEL_PATH)")
    parser.add_argument("--model-id", dest="model_id", help="Model folder id (overrides CODE_TALKER_MODEL_ID)")
    parser.add_argument("--port", dest="port", type=int, default=8000, help="Port to listen on")
    parser.add_argument("--host", dest="host", default="127.0.0.1", help="Host to bind to")
    args, _ = parser.parse_known_args()
    return args


def start_uvicorn_from_cli():
    args = parse_cli_args()
    global _model_path
    _model_path = resolve_model_path(args.model_root, args.model_id)
    import uvicorn
    print(f"[embedding_shim] starting on {args.host}:{args.port} with model_path={_model_path}")

    # Kick off background initialization so /health returns fast and /ready can be polled
    try:
        import threading

        t = threading.Thread(target=_background_load, daemon=True)
        t.start()
    except Exception as e:
        print(f"[embedding_shim] failed to start background loader: {e}")

    uvicorn.run(app, host=args.host, port=args.port, reload=False)


if __name__ == "__main__":
    start_uvicorn_from_cli()

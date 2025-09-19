#!/usr/bin/env python3
"""Download a Hugging Face snapshot of a model into the repo models directory.

Usage: run this script from the repo root. It will create `src/main/resources/models/<model-id>`
"""
import os
import sys
from pathlib import Path

MODEL_ID = "sentence-transformers/all-MiniLM-L6-v2"
OUT_DIR = Path("src/main/resources/models") / MODEL_ID.replace("/","_")

try:
    from huggingface_hub import snapshot_download
except Exception:
    print("huggingface_hub not found: installing via pip...")
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "huggingface-hub"])
    from huggingface_hub import snapshot_download

print("Downloading model:", MODEL_ID)
OUT_DIR.mkdir(parents=True, exist_ok=True)

# Use HF_TOKEN if available
hf_token = os.environ.get("HF_TOKEN")
kwargs = {"repo_id": MODEL_ID, "cache_dir": str(OUT_DIR)}
if hf_token:
    kwargs["use_auth_token"] = hf_token

path = snapshot_download(**kwargs)
print("Downloaded to:", path)
print("Model files at:", list(OUT_DIR.glob("**/*"))[:10])

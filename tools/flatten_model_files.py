#!/usr/bin/env python3
"""Copy runtime-essential files from the HF snapshot into a flatter model folder for Java/REST loading.
Runs from the repo root.
"""
from pathlib import Path
import shutil

SNAPSHOT_BASE = Path("src/main/resources/models/sentence-transformers_all-MiniLM-L6-v2")
OUT_DIR = Path("src/main/resources/models/all-MiniLM-L6-v2")
ESSENTIALS = [
    "config.json",
    "config_sentence_transformers.json",
    "tokenizer_config.json",
    "tokenizer.json",
    "vocab.txt",
    "merges.txt",
    "model.onnx",
    "model.safetensors",
    "pytorch_model.bin",
    "tf_model.h5",
]

OUT_DIR.mkdir(parents=True, exist_ok=True)

# search snapshot dir
snapshots_dir = None
for p in SNAPSHOT_BASE.glob("**/snapshots/*"):
    if p.is_dir():
        snapshots_dir = p
        break

if snapshots_dir is None:
    print("Snapshot directory not found under", SNAPSHOT_BASE)
    exit(1)

copied = []
for name in ESSENTIALS:
    for file in snapshots_dir.rglob(name):
        target = OUT_DIR / file.name
        print(f"Copying {file} -> {target}")
        shutil.copy2(file, target)
        copied.append(target)
        break

if not copied:
    print("No essential files were found in snapshot; listing snapshot root:")
    for f in snapshots_dir.iterdir():
        print(" -", f.name)
else:
    print("Copied files:")
    for c in copied:
        print(" -", c)
print("Done.")

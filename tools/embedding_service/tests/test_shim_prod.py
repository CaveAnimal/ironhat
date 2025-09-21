import sys
import time
from pathlib import Path
from fastapi.testclient import TestClient

# Ensure embedding_service package dir is on sys.path so `import app` works when pytest runs from repo root
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from app import app

client = TestClient(app)

def test_health_ready_embed():
    r = client.get("/health")
    assert r.status_code == 200
    assert "model_path" in r.json()

    # ready may take a short time while background loader runs; timeout=1 to avoid long CI waits
    r2 = client.get("/ready?timeout=1")
    assert r2.status_code in (200, 503)

    # Test embed fake mode: post with simple text
    r3 = client.post("/embed", json={"texts": ["hello world"]})
    assert r3.status_code == 200
    data = r3.json()
    assert "vectors" in data
    assert isinstance(data["vectors"], list)

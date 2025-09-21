import json
import time
from fastapi.testclient import TestClient

from tools.embedding_service.app import app


client = TestClient(app)


def test_health_and_ready_and_fake_embed():
    # health should be up
    r = client.get("/health")
    assert r.status_code == 200
    data = r.json()
    assert "model_path" in data

    # ready endpoint should return a boolean quick (we didn't load runtimes in tests)
    r = client.get("/ready?timeout=1")
    assert r.status_code == 200
    rd = r.json()
    assert isinstance(rd.get("ready"), bool)

    # fake embed behavior: two texts -> two vectors of length 384
    payload = {"texts": ["hello", "world"]}
    r = client.post("/embed", json=payload)
    assert r.status_code == 200
    j = r.json()
    assert "vectors" in j
    vectors = j["vectors"]
    assert isinstance(vectors, list) and len(vectors) == 2
    assert all(isinstance(v, list) for v in vectors)
    assert len(vectors[0]) == 384
    # deterministic: calling again yields same first element
    r2 = client.post("/embed", json=payload)
    assert r2.json()["vectors"][0] == vectors[0]

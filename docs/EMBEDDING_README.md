# Embedding REST shim

This folder provides a lightweight Python FastAPI shim that loads the `all-MiniLM-L6-v2` model from `src/main/resources/models/all-MiniLM-L6-v2` and exposes a `/embed` endpoint.

Quickstart

1. Create a virtualenv and install requirements:

```powershell
python -m venv .venv; .\.venv\Scripts\Activate.ps1; pip install -r tools/embedding_service/requirements.txt
```

2. Run the service:

```powershell
uvicorn tools.embedding_service.app:app --reload --host 127.0.0.1 --port 8000
```

3. Example request (PowerShell):

```powershell
$body = '{"texts":["hello world","some code snippet"]}'
Invoke-RestMethod -Uri http://localhost:8000/embed -Method POST -Body $body -ContentType "application/json"
```

Java client snippet

```java
String jsonPayload = "{\"texts\":[\"hello world\"]}";
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:8000/embed"))
    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
    .header("Content-Type", "application/json")
    .build();

HttpResponse<String> resp = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
// parse with Jackson into a POJO: { "vectors": float[][] }
```

Notes

- The service loads the local model directory; if you need GPU support or different backends, change the model loading implementation accordingly.
- The flattened model files are in `src/main/resources/models/all-MiniLM-L6-v2`.

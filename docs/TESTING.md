# Testing and Integration Guide

This document explains how to run unit and integration tests locally, with PowerShell-safe commands and fast troubleshooting tips.

Prerequisites
- Java 21 (or the JDK used by the project). Ensure `JAVA_HOME` is set and `mvn -v` shows the expected JDK.
- Maven 3.8+ installed and available on `PATH`.
- Python (optional): used by the embedding shim. The project tests can auto-start the shim when needed.

Common environment variables
- `CODETALKER_MODEL_PATH` or `-Dcodetalker.model.path` -- path to a SavedModel or TFLite file to use for TF integration tests.
- `HF_TOKEN` -- (optional) Hugging Face token for downloading HF model snapshots when needed.

PowerShell-friendly commands

1) Run unit tests (PowerShell):

```powershell
Set-Location -LiteralPath 'E:\MyProjects\MyGitHubCopilot\ironhat\FeH-001'
# Run unit tests only
mvn test
```

Running a single test from PowerShell

PowerShell's parsing can interfere with `-D` arguments when invoking Maven to run a single test by FQN. Use `cmd` with stop-parsing (`--%`) to ensure the `-Dtest=` value is passed literally. Example:

```powershell
Set-Location -LiteralPath 'E:\MyProjects\MyGitHubCopilot\ironhat\FeH-001'
cmd /c mvn --% -Dtest=com.codetalker.embedding.EmbeddingBinaryRepositoryUpsertTest test
```

This pattern is also useful when you need to pass other `-D` system properties that PowerShell might otherwise reinterpret.


2) Run integration tests that require TensorFlow/native artifacts

PowerShell quoting for `-D` properties can be tricky. Preferred approach is to set an environment variable then pass it into Maven. Example (PowerShell):

```powershell
Set-Location -LiteralPath 'E:\MyProjects\MyGitHubCopilot\ironhat\FeH-001'
$env:CODETALKER_MODEL_PATH = 'E:\MyProjects\MyGitHubCopilot\ironhat\FeH-001\src\main\resources\models\universal-sentence-encoder-lite.tflite'
mvn -Pwith-tensorflow -Dcodetalker.model.path="$env:CODETALKER_MODEL_PATH" verify
```

If PowerShell argument parsing still causes issues, use `cmd.exe` with stop-parsing to pass the `-D` literally (this is how I ran the integration in this environment):

```powershell
Set-Location -LiteralPath 'E:\MyProjects\MyGitHubCopilot\ironhat\FeH-001'
cmd /c mvn -Pwith-tensorflow --% -Dcodetalker.model.path="E:\MyProjects\MyGitHubCopilot\ironhat\FeH-001\src\main\resources\models\universal-sentence-encoder-lite.tflite" verify
```

3) Force Maven to re-resolve dependencies

```powershell
cmd /c mvn -U -Pwith-tensorflow --% -Dcodetalker.model.path="E:\MyProjects\MyGitHubCopilot\ironhat\FeH-001\src\main\resources\models\universal-sentence-encoder-lite.tflite" verify
```

Troubleshooting tips
- If Maven fails with dependency resolution errors that mention relocations (POM relocation to a different groupId), the artifact may not be published to Maven Central. Check `ironhat_POM_UPDATES.md` for prior notes or add the needed repository to `pom.xml`.
- If a repository host (e.g., Bytedeco) is not reachable, remove or comment the related fallback dependency and re-run the build to isolate the failure.
- If native library loading fails at runtime (UnsatisfiedLinkError) you may need platform-specific JNI binaries. The project attempts reflective fallbacks; if necessary, document a local install of the platform bundle into `~/.m2/repository`.
- For embed-shim issues: the integration tests will attempt to contact `127.0.0.1:8000` and will auto-start the Python shim if it's not reachable. To run the shim manually:

```powershell
Set-Location -LiteralPath 'E:\MyProjects\MyGitHubCopilot\ironhat\FeH-001'
# Example: start a shim (project contains scripts to download and run model shims)
python tools/download_model.py --model all-MiniLM-L6-v2 --out src\main\resources\models\all-MiniLM-L6-v2
# Then run the shim if available (this is example; integration tests auto-start their own shim by default)
python -m some_shim_module --model-path src\main\resources\models\all-MiniLM-L6-v2
```

Recording issues
- If you modify `pom.xml` to add repositories or dependencies, update `ironhat_POM_UPDATES.md` with the timestamp and rationale.

If you want, I can add a short `scripts` helper that standardizes running the integration locally (PowerShell + cmd fallback). I can add it under `scripts/` and update the docs — say "add script" and I'll create it.

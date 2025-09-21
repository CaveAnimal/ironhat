
MODEL PROVISIONING
===================

Purpose
-------
This document explains how runtime ML models are provisioned for local development and CI, why they are excluded from the normal Maven resource copy, and how to package them explicitly when desired.

Why models are excluded from the jar
-----------------------------------
- Model artifacts (ONNX, gguf, large PyTorch/TF files, tokenizer snapshots, etc.) are large and often open by external processes (Python shims, runtime servers). On Windows this can lead to FileSystemException errors during build (Maven trying to copy or package files that are locked).
- To avoid huge jars and platform-specific file-lock failures, `pom.xml` is configured to exclude `src/main/resources/models/**` from the normal resources phase by default.

Where the code expects models at runtime
--------------------------------------
- By default, the Python embedding shim and Java integration tests look for models under the repository path `src/main/resources/models/<model-id>` during development.
- In production or CI it is recommended to place models on the filesystem outside of the application jar, and point the application to that path via an environment variable or system property.

Recommended runtime model location (example)
---------------------------------------------
- Local development: keep `src/main/resources/models/<model-id>` for convenience but do not commit large artifacts to the repo. The repository's `tools/download_hf_model.py` and `tools/flatten_model_files.py` can be used to create the flattened model layout for development.
- CI / server: provision models into a directory accessible to the runtime, e.g. `E:/models/code-talker/<model-id>` (Windows) or `/opt/models/code-talker/<model-id>` (Linux).

Configuration options
---------------------
1) Environment variable (recommended):
   - `CODE_TALKER_MODEL_PATH` — absolute path to a directory that contains the flattened model folder(s).
   - The Java app and the Python shim should check this env var first; if present use it instead of `src/main/resources/models`.

2) System property (JVM):
   - `-Dcode.talker.model.path=/opt/models/code-talker` when launching the Java process or in the Maven surefire/failsafe plugin system properties for tests.

3) Maven profile to package models (manual):
   - If you really need to create a build artifact containing models (not recommended), enable a special Maven profile which will override the `resources` excludes. Example profile name: `package-models`. This must be invoked explicitly and should not be used in CI by default.

How to run locally (dev)
-------------------------
1. Create the venv and install the Python shim deps (if you use the shim):

```powershell
python -m venv .venv
.\\.venv\\Scripts\\Activate.ps1
pip install -r tools/embedding_service/requirements.txt
```

2. Place or copy the flattened model into `src/main/resources/models/<model-id>` (development convenience only).
   - Use `tools/download_hf_model.py` and `tools/flatten_model_files.py` to download / flatten the HF model into that location.

3. Start the shim (optional) using the runtime model path if you set `CODE_TALKER_MODEL_PATH`:

```powershell
# use CODE_TALKER_MODEL_PATH or default path inside repo
$env:CODE_TALKER_MODEL_PATH='src/main/resources/models'
.\\.venv\\Scripts\\Activate.ps1; python -m uvicorn tools.embedding_service.app:app --host 127.0.0.1 --port 8000
```

How to run in CI or production (recommended)
--------------------------------------------
- Provision models on the host or CI agent into a directory, and set `CODE_TALKER_MODEL_PATH` to that directory in the environment for the service or test job.
- For CI unit/integration runs that require the shim, either:
  - Start a separate service in CI that serves embeddings (recommended) and point tests to it, or
  - Use the `ci-no-shim` Maven profile to disable tests auto-starting the local shim and instead rely on the provided service.

Packaging models intentionally
-----------------------------
- If you need a distributable artifact that includes models, add a dedicated Maven profile `package-models` that removes the `models/**` exclude (or includes a separate `src/main/resources/models-shipped` directory). This must be run explicitly; example:

```powershell
mvn -Ppackage-models clean package
```

Notes and gotchas
-----------------
- Do not rely on the repository's `src/main/resources/models` to hold production-sensitive models. Use a secured, versioned model repository or artifact store for production.
- On Windows, processes may lock files in ways that prevent Maven from copying or deleting them. Keeping models out of `target/classes` avoids many locking problems during development.
- The integration tests in this repo will auto-start the Python shim by default. Control this behavior with the Maven profile `ci-no-shim` or by setting the system property `embed.shim.autoStart=false`.

Contact
-------
If you'd like me to also add a `package-models` Maven profile that packages models only when explicitly enabled (with a short README excerpt showing the command), tell me and I will add it and run a quick build to verify.

CI job examples
---------------
Example GitHub Actions job that provisions models from a secure artifact store and runs tests:

```yaml
name: CI
on: [push, pull_request]
jobs:
   test:
      runs-on: ubuntu-latest
      steps:
         - uses: actions/checkout@v4
         - name: Download model artifact
            run: |
               mkdir -p /tmp/models
               # Replace with your fetch command (curl, aws s3 cp, gh release download, etc.)
               curl -L -o /tmp/models/all-MiniLM-L6-v2.zip "${{ secrets.MODEL_ARTIFACT_URL }}"
               unzip -d /tmp/models /tmp/models/all-MiniLM-L6-v2.zip
         - name: Run tests with model path
            env:
               CODE_TALKER_MODEL_PATH: /tmp/models
            run: mvn -DskipTests=false verify
```

Example Azure Pipelines job (Linux) that provisions models and runs Maven with the env var:

```yaml
trigger:
   - main
pool:
   vmImage: 'ubuntu-latest'
steps:
- checkout: self
- script: |
      mkdir -p /tmp/models
      # Download model to /tmp/models using your organization's secured method
      curl -L -o /tmp/models/all-MiniLM-L6-v2.zip $(MODEL_ARTIFACT_URL)
      unzip -d /tmp/models /tmp/models/all-MiniLM-L6-v2.zip
   displayName: 'Fetch model'
- script: |
      export CODE_TALKER_MODEL_PATH=/tmp/models
      mvn -DskipTests=false verify
   displayName: 'Run Maven tests'
```

Notes:
- Use your CI provider's secure artifact storage (release assets, S3 with restricted credentials, Azure Artifacts) and secrets management to avoid embedding model URLs or credentials in the pipeline YAML.
- For Windows runners, adjust the commands to use PowerShell and Windows paths and set `CODE_TALKER_MODEL_PATH` accordingly.

Testing note (opt-in assertions)
--------------------------------
If your CI job provisions a real model (for example an ONNX or PyTorch exported embedding model) you can make the Java integration tests assert that the shim actually loaded that runtime by passing system properties to Maven after the model is available in `CODE_TALKER_MODEL_PATH`:

```powershell
# require loaded runtime and expect ONNX in CI
mvn -DskipTests=false -Dembed.shim.requireLoaded=true -Dembed.shim.expectRuntime=onnx test
```

This keeps local developer runs lightweight while allowing CI to validate real runtime integration when desired.

GitHub Actions example
----------------------
An example GitHub Actions workflow `./github/workflows/ci-provision-models.yml` (also included in this repo) demonstrates a minimal pattern:

- Store the model artifact URL as a repository secret named `MODEL_ARTIFACT_URL` (or use a more secure fetch step from your artifact store).
- The workflow downloads and unpacks the model to `/tmp/models`, sets the env var `CODE_TALKER_MODEL_PATH` and runs Maven with the opt-in assertion flags.

Note: The example workflow uses an archive (`.zip`) artifact that contains the flattened model layout expected by the shim (a folder containing model files and tokenizer files). Adjust the extraction step to match your artifact format.

Provider-specific download examples
----------------------------------
The workflows support several secure download methods. Pick one and configure the corresponding secrets in your repository/organization settings.

1) GitHub Releases
    - Secrets required:
       - `MODEL_ARTIFACT_SOURCE=github_release`
       - `MODEL_RELEASE_REPO` (owner/repo)
       - `MODEL_RELEASE_TAG` (release tag or `latest`)
       - `MODEL_RELEASE_ASSET` (asset name or glob)
    - Notes:
       - The workflow uses the `gh` CLI's `release download` command. Ensure the `GITHUB_TOKEN` or a PAT with repo access is available to the runner (GitHub Actions provides `GITHUB_TOKEN` automatically for workflows in the same repo).

2) AWS S3
    - Secrets required:
       - `MODEL_ARTIFACT_SOURCE=s3`
       - `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, and `AWS_REGION`
       - `MODEL_S3_URI` (e.g. `s3://my-bucket/path/to/model.zip`)
    - Notes:
       - The workflow uses the `aws` CLI to copy the artifact from S3. Make sure the runner has network access and the IAM credentials allow `s3:GetObject` for the target object.

Fallback: Generic HTTP/HTTPS
    - Secrets required:
       - `MODEL_ARTIFACT_SOURCE=http` (or unset)
       - `MODEL_ARTIFACT_URL` — direct URL to the artifact (signed URL recommended)
    - Notes:
       - The workflow uses `curl` (Linux) or `Invoke-WebRequest` (Windows) to download the artifact. Signed URLs or short-lived tokens are recommended for security.

Automatic layout normalization (CI)
---------------------------------
The provided CI workflows include a small normalization step that helps handle common artifact variations:

- If the extracted model archive places the model files under a single top-level folder (for example `all-MiniLM-L6-v2/...`), the workflow will automatically move that folder's contents up into the configured model directory (e.g. `/tmp/models` or `%TEMP%\models`).
- The normalization is conservative: it only runs when the target directory contains exactly one subdirectory and no top-level files. This prevents accidental merges and keeps the behavior predictable.
- After normalization the workflow runs a validation step that checks for either `model.onnx` or `tokenizer.json` and fails fast with a recursive listing if the expected files are not present.

Windows specifics
-----------------
- The Windows workflow uses PowerShell and writes the model into `%TEMP%\models`. The normalization and validation steps use PowerShell (`pwsh`) to ensure cross-runner compatibility.
- If your artifact provider returns different archive shapes (nested folders, multiple top-level directories), consider producing flattened artifacts or update the workflow's normalization logic to match your packaging.

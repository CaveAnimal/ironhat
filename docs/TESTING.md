# Testing and CI notes

This document contains testing guidance for the repository and examples for CI jobs that may need to exercise the embedding shim or require a specific embedding runtime.

## Opt-in runtime assertions for the embedding shim

The Java integration test `EmbedServiceIntegrationTestIT` now queries the embedding shim's `/metrics` endpoint and supports optional, opt-in assertions that make CI stricter when you want to validate a real runtime is available.

Available system properties (Maven -D):

- `embed.shim.requireLoaded` (boolean) — when `true`, the integration test will fail if `/metrics` reports `loaded: false`. Use this when your job provisions a real runtime (e.g., installs `sentence-transformers` or provides ONNX artifacts).
- `embed.shim.expectRuntime` (string) — when set (examples: `st` or `onnx`), the integration test will fail if `/metrics` reports a different `runtime` value.

By default these properties are not set and tests will only log `/metrics` for diagnostics. This keeps local developer runs and lightweight CI jobs tolerant of the shim's fake fallback.

## Example: GitHub Actions job that requires the shim to have loaded a runtime

This example assumes you provision a model or install the runtime in CI before running Maven. Replace the model provisioning step with your organization's secure artifact fetch.

```yaml
name: CI
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Provision model artifacts
        run: |
          mkdir -p /tmp/models
          # fetch and unpack your model(s) into /tmp/models
          # e.g. curl -L -o /tmp/models/model.zip "${{ secrets.MODEL_ARTIFACT_URL }}" && unzip -d /tmp/models /tmp/models/model.zip
      - name: Run Maven tests (require runtime)
        env:
          CODE_TALKER_MODEL_PATH: /tmp/models
        run: |
          mvn -DskipTests=false -Dembed.shim.requireLoaded=true -Dembed.shim.expectRuntime=st test
```

## Example: Windows PowerShell runner (local or CI) — require runtime

PowerShell one-liner for CI or an ad-hoc job that sets the model path and requires the runtime:

```powershell
$env:CODE_TALKER_MODEL_PATH = 'E:\agents\models'
mvn -DskipTests=false -Dembed.shim.requireLoaded=true -Dembed.shim.expectRuntime=st test
```

## Notes and troubleshooting

- If you see failures with `embed.shim.requireLoaded=true`, check the shim logs in `target/failsafe-reports/EmbedServiceIntegrationTestIT-shim.log` or the console output prefixed with `[embed-shim-metrics]`.
- For lightweight CI that should not install heavy runtimes, leave the properties unset and rely on the deterministic fake fallback for hermetic tests.
- If you prefer ONNX in CI (lighter than PyTorch), package or provision the ONNX model files into `CODE_TALKER_MODEL_PATH` and set `embed.shim.expectRuntime=onnx`.

If you want, I can add a short GitHub Action that provisions an ONNX model artifact from a release asset and demonstrates a green run — tell me which model artifact URL or repository to use.

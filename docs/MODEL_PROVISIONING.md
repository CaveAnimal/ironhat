Model provisioning guidance

If the automated downloader (`scripts/download_model.ps1`) fails to extract the model, follow these manual steps to obtain the Universal Sentence Encoder Lite (TFLite) model and place it under `src/main/resources/models/`.

1. Open the model page in your browser:
   - https://tfhub.dev/google/lite-model/universal-sentence-encoder-lite/1

2. Click the "Download" or "Files" link on the TF Hub page and download the compressed archive (it may be a .tar.gz or .zip). TF Hub sometimes serves content via redirects or requires an authenticated flow which `Invoke-WebRequest` may not follow cleanly.

3. Extract the archive locally and locate the `.tflite` file (usually named something like `model.tflite` or similar).

4. Copy the `.tflite` file into the project:

```
# from project root (PowerShell)
mkdir -Force src\main\resources\models
copy \path\to\downloaded\model.tflite src\main\resources\models\
```

5. Optionally, rename the model file to `use-lite.tflite` or update `ModelLoader` to point to the filename you used.

6. Re-run the tests to verify the model loads:

```
# from project root
mvn -DskipTests=false clean test
```

Notes and troubleshooting
- If TF Hub serves a page instead of a direct archive, inspect the page for a link to `storage.googleapis.com` where the model files are hosted — these are direct static URLs you can use.
- If you need strict automation in CI, consider checking the model into a separate artifact repository or using a release asset in your organization that Maven/CI can access.
- If downloading large model files into the repo is undesirable, maintain an external provisioning script or artifact registry and document credentials/access in your CI configuration.

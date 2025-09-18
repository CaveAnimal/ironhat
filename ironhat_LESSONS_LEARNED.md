# Lessons Learned — Obtaining TensorFlow/TFLite artifacts

This document records challenges, observations, and recommendations from attempting to add TensorFlow and TensorFlow Lite artifacts to the `ironhat` project (branch `FeH-001`). It is intended to help future developers quickly understand the pitfalls and options when integrating TensorFlow Java runtimes.

## Summary of Key Challenges

1. Artifact coordinates and relocations
   - Many TensorFlow-related artifacts have moved coordinates or been relocated across releases. For example, `org.tensorflow:tensorflow-lite:2.17.0` on Maven Central contains a POM relocation to `com.google.ai.edge.litert:litert:1.0.1`. Relocations can cause dependencies to point at group/artifact coordinates that are not present in the configured repositories, which breaks resolution.

2. Missing or platform-specific JNI bundles
   - TFLite native JNI artifacts (e.g., `tensorflow-lite-select-jni`) may not be published to Maven Central for all versions, or may be published only in Google Maven, or with platform-specific classifiers. That means the Java API JARs may be available but the actual native runtime binaries are missing, leaving runtime failure unless the native library is installed separately.

3. Repository differences (Central vs Google Maven)
   - Some TensorFlow artifacts are published to Google Maven (`https://maven.google.com`) rather than Maven Central. Projects must include the correct repositories in the POM to resolve these artifacts. Even with the Google Maven repository listed, some relocated group/artifacts may not be present publicly.

4. Version compatibility and transitive dependencies
   - TensorFlow Java artifacts have many transitive dependencies and sometimes strict version constraints. Pulling a large `tensorflow-core-platform` package can significantly expand the dependency tree and bring in native binaries for different platforms. This can complicate CI builds and increase artifact download times.

5. Local cache and cached failures
   - Maven caches failed resolution attempts in the local repository. If a dependency fails to resolve once, subsequent builds may fail quickly unless `-U` (force update) is used or the specific cached entries are purged. This behavior can be confusing during iterative experimentation.

6. Relocation/renaming to internal groups
   - The project observed artifacts being relocated to groups like `com.google.ai.edge.litert` which may be internal or not published to public repositories. Resolving such relocations may require additional repository configuration or contacting the artifact publisher.

## Practical Mitigations and Recommendations

1. Prefer platform-agnostic Java API artifacts for development
   - Use `tensorflow-lite` and `tensorflow-lite-api` for compile-time development. Accept that native JNI support may be missing in CI and provide a fallback or mock for tests requiring native runtime.

2. For runtime/native testing use `tensorflow-core-platform` only when necessary
   - `tensorflow-core-platform` bundles many native components and can reduce fragmentation, but it is large. Use it in integration stages or developer machines rather than lightweight unit test stages.

3. If a required JNI artifact is missing, use one of:
   - Add the repository that hosts the native JAR (if public).
   - Manually install the native JAR into the local Maven repository with `mvn install:install-file` (document platform classifier and checksum).
   - Use Bytedeco or another third-party distribution if it better suits your build environment (note license and behavior differences).

4. Always include Google Maven in `pom.xml` if depending on TensorFlow
   - Example:
     ```xml
     <repository>
       <id>google-maven</id>
       <url>https://maven.google.com</url>
     </repository>
     ```

5. When experimenting, purge cached failures or run with `-U`
   - Commands: `mvn dependency:purge-local-repository -DmanualInclude="org.tensorflow:*" -DreResolve=true` or use `mvn -U`.

6. Log every POM edit and build attempt
   - Keep a file such as `ironhat_POM_UPDATES.md` (already implemented) that records every POM change, reason, timestamp, and build outcome. This prevents flip-flopping and makes experiments reproducible.

7. Prefer classifier-based native artifacts or containerized/native images for production
   - For production deployments where native performance matters, consider packaging native dependencies in containers or using platform-specific installers instead of relying on Maven to pull platform-specific artifacts at build time.

## Actionable checklist for future integration

- [ ] Decide whether to use official TensorFlow artifacts or Bytedeco.
- [ ] If official TF is required: identify which JNI artifacts are required for your target platforms and whether they are present in Google Maven.
- [ ] Automate a step in CI to install missing native JARs or use a platform matrix with platform-specific artifacts.
- [ ] Add documentation and a small helper script for manually installing the JNI jars into the local Maven repo.

## Example local install command (if you have the JNI jar)

mvn install:install-file -Dfile=path/to/libtensorflow-lite-select-jni-<version>-<classifier>.jar -DgroupId=org.tensorflow -DartifactId=tensorflow-lite-select-jni -Dversion=<version> -Dpackaging=jar -Dclassifier=<classifier>



---
Last updated: 2025-09-17T12:10:00-05:00

## Model provisioning: challenges and what worked

While attempting to provision a TensorFlow Lite model (Universal Sentence Encoder Lite) for local development we encountered several practical hurdles. Below are the challenges we hit and the approaches that worked for obtaining a usable `.tflite` file in a reproducible way.

Challenges observed
- TF Hub download flow is not always a simple static archive link. The TF Hub page frequently uses a redirect flow or an interactive download widget that returns HTML pages rather than direct `.zip` or `.tflite` bytes when fetched with a simple HTTP client.
- `Invoke-WebRequest` and other naive HTTP fetches can receive an HTML landing page (or a small HTML redirect) instead of the expected archive, causing extraction errors such as "End of Central Directory record could not be found.".
- Some TF Hub endpoints return compressed files in formats not expected by the script (e.g., `tar.gz` instead of `.zip`), or require following JavaScript-driven redirects which are not followed by basic HTTP clients.

What worked (practical solutions)
- Manual download from TF Hub UI: Open the model page (`https://tfhub.dev/google/lite-model/universal-sentence-encoder-lite/1`), use the site's download link and extract the `.tflite` file locally. Then copy the `.tflite` into `src/main/resources/models/`. This is the simplest and most reliable approach for developers.
- Use direct `storage.googleapis.com` URLs when available: TF Hub model assets are commonly hosted on Google Cloud Storage; inspect the TF Hub page or network trace for a `storage.googleapis.com` link to a `.tflite` file and download that directly with `Invoke-WebRequest` or `curl`.
- Add the model as a project artifact or release asset: For CI reproducibility, store the `.tflite` as a release asset in your GitHub releases (or internal artifact repository). CI can download it via a stable URL or authenticated API rather than relying on TF Hub's UI flow.
- Script improvements (partial): If automation is required, enhance the downloader to follow HTTP redirects, detect `tar.gz` vs `zip` archives, and handle extraction accordingly. However, this adds complexity and can still fail for JS-driven flows.

Recommended reproducible approach (developer + CI)
1. For local dev: manual download from TF Hub UI or direct `storage.googleapis.com` link. Place the `.tflite` file at `src/main/resources/models/` and commit a small README explaining the filename and source.
2. For CI: upload the vetted `.tflite` file as a release asset or host it in an internal storage bucket accessible to CI; update `docs/MODEL_PROVISIONING.md` with the CI download commands and authentication notes.
3. If you must attempt full automation against TF Hub: implement robust redirect following, support multiple archive formats, and add retries; prefer using `curl` with `-L` to follow redirects and inspect the final URL for `storage.googleapis.com`.

Small example `curl` snippet that follows redirects and writes the final target to `models/use-lite.tflite` if a direct file link is reached:

```powershell
curl -L "https://tfhub.dev/google/lite-model/universal-sentence-encoder-lite/1?tf-hub-format=compressed" -o temp-download
# Inspect temp-download: if it is a zip use Expand-Archive, if tar.gz use tar -xzf, otherwise check for HTML and follow final redirect URL
```

Last updated: 2025-09-17T13:37:00-05:00

## PowerShell pitfalls encountered while automating downloads

During attempts to automate TF Hub model downloads we repeatedly ran into issues caused by PowerShell-specific behavior and quoting complexities. Recording these problems and the safe alternatives will save time for future automation work.

Common mistakes we made
- Using PowerShell's `curl` alias inadvertently: On Windows PowerShell, `curl` is an alias for `Invoke-WebRequest` which behaves differently from the native `curl.exe` (different flags, different default behavior on redirects and output). That caused unexpected parameter binding and HTML responses instead of raw file bytes.
- Attempting very complex one-liner `-Command` invocations: Building large, quoted PowerShell expressions inline led to parsing errors (for example errors about `-f` or missing expressions) because nested quoting and format operators require careful escaping in `-Command` strings.
- Assumed downloaded bytes were archives: `Invoke-WebRequest`/`curl` calls sometimes returned small HTML pages (redirects, UI) and our scripts tried to `Expand-Archive` them, producing "End of Central Directory" errors.
- Formatting byte arrays incorrectly: When converting bytes to hex we used format strings inside pipelines without proper expression context, which produced parser errors like "You must provide a value expression following the '-f' operator.".

Best practices and fixes
- Use `curl.exe` explicitly for HTTP fetches that rely on following redirects and binary-safe output. Example:

```powershell
curl.exe -L $url -o .\src\main\resources\models\temp-download
```

- Prefer small, single-purpose PowerShell scripts (saved `.ps1` files) instead of extremely long `-Command` one-liners; this avoids nesting/escaping pitfalls and makes debugging easier.
- Inspect the downloaded file header before attempting extraction. Check the first few bytes and detect common signatures: ZIP `50 4B`, GZIP `1F 8B`, or HTML (`<!DOCTYPE`/`<html`). Use `[System.IO.File]::OpenRead()` to read bytes safely in PowerShell scripts.
- Use `Expand-Archive` for ZIP and `tar` for `tar.gz` on Windows 10/11 environments that include `tar`. If those fail, fall back to manual extraction tools and document the required tooling.
- When formatting byte values to hex in PowerShell, use a pipeline expression like:

```powershell
$bytes | ForEach-Object { '{0:X2}' -f $_ }
```

- When automating downloads from TF Hub prefer to discover and use direct `storage.googleapis.com` links (if visible) or host the model as a CI-accessible release asset; don't rely on the TF Hub UI download flow for fully automated scripts.

Recording these details here will help avoid repeated trial-and-error when we or CI try to automate model provisioning.

Last updated: 2025-09-17T13:55:00-05:00

# ironhat
Just some stuff I gotta do

## Benchmarks and summarizer notes

## TensorFlow integration (optional)

The project includes an opt-in Maven profile that pulls the (large) TensorFlow Java native artifacts so you can run integration tests that exercise real model-loading code.

- Profile id: `with-tensorflow`
- Activation: use `-Pwith-tensorflow` or pass the property `-DwithTensorFlow=true` on the Maven command line.

Why opt-in?
- The TensorFlow Java distribution is large and platform-specific (native libraries). Keeping it out of the default dependency set keeps developer builds fast and portable; enable it only when you need to run integration tests.

Example commands

Run the regular unit tests (no TF):

```powershell
mvn -DskipTests=false test
```

Run tests with TensorFlow dependencies enabled (may pull large native artifacts):

```powershell
mvn -Pwith-tensorflow -DskipTests=false test
# or equivalently
mvn -DwithTensorFlow=true -DskipTests=false test
```

Notes and platform requirements
- Native libraries: the TensorFlow Java artifacts include platform-specific native libraries. On some platforms you may need additional system libraries or a matching CPU/GPU runtime. If tests fail with UnsatisfiedLinkError or similar, the platform-native libs are likely incompatible or missing.
- Guarded loading: `ModelLoader` attempts to initialize TensorFlow (TFLite `Interpreter` or the full `SavedModelBundle`) reflectively only if TF classes are present. If the TF runtime is not on the classpath the loader will fall back to treating the model file presence as a successful load (or mock mode). This keeps the default test runs lightweight.
- Mock mode for unit tests: to run tests without native model dependencies, you can set the system property `codetalker.model.mock=true`. Some unit tests also set this property internally where appropriate.

Troubleshooting
- If you see reflective initialization warnings during tests (SavedModelBundle or TFLite constructor/method not found), it usually means the TF Java version on the classpath exposes different APIs. We intentionally ignore these warnings and fall back; if you want full TF integration, share the exact TF version/platform you plan to use and I can adapt the reflective loader to that API.

If you want, I can (a) move the TensorFlow dependency fully out of the main `<dependencies>` into the `with-tensorflow` profile (so it's never downloaded for default builds), (b) add a dedicated integration-test profile to run only tests that need the TF runtime, or (c) implement additional reflective signatures for other TF versions. Which would you prefer?

The `benchmarks` scripts produce aggregated sweep CSVs and condensed summaries. When you run the sweep runner with `-AutoSummarize` or call the summarizer directly, it writes `benchmarks/best_table.csv` which contains a compact selection per `m` (rows: `m,ef,recallUsed,comment,avgQueryMs,buildMs,memoryBytes`).

Annotation: when the summarizer cannot find any `ef` value that meets the configured recall threshold for a given `m`, but the `-bestByMaxRecall` flag is enabled, the summarizer will choose the `ef` with the highest recall as a fallback. Those rows will have the `comment` field include the suffix `(max-recall fallback)` so you can tell which selections were chosen by fallback logic.

Example (excerpt from `benchmarks/best_table.csv`):

```
m,ef,recallUsed,comment,avgQueryMs,buildMs,memoryBytes
8,200,0.9400,recall=0.9400 (threshold=0.95) (max-recall fallback),0.396245,1743,6722912
```

This indicates that for `m=8` the `ef=200` row was picked because it had the highest recall available, but its recall (0.94) is below the requested threshold (0.95).

If you prefer the summarizer to fail-to-select instead of using the fallback, omit the `-bestByMaxRecall` flag when invoking the summarizer.

Running TF integration tests (quick guide)
----------------------------------------

1) Enable the profile and run the integration/verification phase (PowerShell):

```powershell
mvn -Pwith-tensorflow -Dcodetalker.model.path="C:\absolute\path\to\saved_model" verify
# or equivalently
mvn -DwithTensorFlow=true -Dcodetalker.model.path="C:\absolute\path\to\saved_model" verify
```

Notes:
- `-Pwith-tensorflow` activates the `with-tensorflow` profile which adds the TensorFlow Java platform dependency and sets `tf.integration.enabled=true` for tests. The integration test skeleton `TFSavedModelIntegrationTest` checks that property and will be skipped when the profile is not active.
- `-Dcodetalker.model.path` should point to a directory containing a SavedModel (the same layout `SavedModelBundle.load(path)` expects) or to a TFLite file if you adapt tests.

2) Obtaining compatible TensorFlow artifacts and models

- Use the `org.tensorflow:tensorflow-core-platform` artifact that the profile declares. This artifact pulls platform-specific native libs; Maven will download the correct native classifier for your OS/arch when available.
- If you need GPU support or a particular native build, obtain the matching TensorFlow Java native package from the TensorFlow maven artifacts (Google Maven) or build the native libs from source. Platform mismatches commonly cause `UnsatisfiedLinkError` at runtime.
- For quick local testing, use small SavedModel exports compatible with the TF Java version (e.g., a simple TF 2.x SavedModel that accepts a 1-D float tensor and returns a 1-D float vector). You can export small test models from Python with:

```python
import tensorflow as tf

# simple model: identity mapping for a given size
inp = tf.keras.Input(shape=(16,))
out = tf.keras.layers.Dense(16, use_bias=False, kernel_initializer=tf.keras.initializers.Identity())(inp)
model = tf.keras.Model(inputs=inp, outputs=out)
model.save('/tmp/simple_saved_model')
```

3) Troubleshooting checklist

- If you see `UnsatisfiedLinkError`: check the native classifier Maven downloaded (look in your local repo under `org/tensorflow`), and ensure it matches your OS/arch. Consider specifying an explicit platform classifier or using the `tensorflow-native` artifact that matches your environment.
- If reflective methods (e.g., `session`, `runner`, `run`) are not found: your TF Java API version may differ; open an interactive REPL and inspect the API or share the exact TF Java version and I can add additional reflective code paths.
- If you don't want to install native libs, use `-Dcodetalker.model.mock=true` to run unit tests with the test stubs and in-JVM fallback.

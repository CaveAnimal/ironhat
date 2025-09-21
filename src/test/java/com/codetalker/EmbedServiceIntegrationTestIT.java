package com.codetalker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.codetalker.testing.MetricsInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EmbedServiceIntegrationTestIT {

    // base URL is determined at runtime: prefer an existing shim at 8000, otherwise start a shim on a free port
    private String baseUrl = null;
    private static final int EXPECTED_DIM = 384;
    private Process shimProcess = null;

    @BeforeEach
    public void ensureShimRunning() throws Exception {
        // Read system properties set via Maven (failsafe configuration)
        String propAutoStart = System.getProperty("embed.shim.autoStart", "true");
        boolean autoStart = Boolean.parseBoolean(propAutoStart);
        String propPython = System.getProperty("embed.shim.python", "");

        // First try connecting to the usual default port 8000; if a shim is already running there we'll reuse it.
        tools.java.EmbedClient probeClient = new tools.java.EmbedClient("http://127.0.0.1:8000");
        try {
            probeClient.embedJson("{\"texts\":[\"ping\"]}");
            this.baseUrl = "http://127.0.0.1:8000";
            return; // running
        } catch (IOException e) {
            if (!autoStart) {
                System.out.println("Embedding shim not reachable and autoStart is disabled; failing setup.");
                throw new IllegalStateException("Embedding shim not reachable and autoStart disabled");
            }

            // start shim
            System.out.println("Embedding shim not reachable, starting local shim for integration test.");
            // Determine python executable: prefer venv (Windows or POSIX), otherwise fallback to 'python' on PATH
            Path winVenv = Path.of(".venv", "Scripts", "python.exe");
            Path posixVenv = Path.of(".venv", "bin", "python");
            String pythonExe = null;
            if (propPython != null && !propPython.isBlank()) {
                pythonExe = propPython;
            } else if (Files.exists(winVenv)) {
                pythonExe = winVenv.toAbsolutePath().toString();
            } else if (Files.exists(posixVenv)) {
                pythonExe = posixVenv.toAbsolutePath().toString();
            } else {
                pythonExe = "python"; // rely on PATH
            }

            // pick a free port to avoid collisions with other processes
            int port;
            try (java.net.ServerSocket ss = new java.net.ServerSocket(0)) {
                port = ss.getLocalPort();
            }

            // ensure target directory exists for log capture
            Path targetDir = Path.of("target");
            try {
                Files.createDirectories(targetDir);
            } catch (IOException ignored) {
            }
            File logFile = targetDir.resolve("failsafe-shim-port-" + port + ".log").toFile();

            ProcessBuilder pb = new ProcessBuilder(pythonExe, "-m", "uvicorn", "tools.embedding_service.app:app", "--host", "127.0.0.1", "--port", Integer.toString(port));
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
            pb.directory(Path.of(".").toFile());
            shimProcess = pb.start();

            // wait up to 20s for shim to start
            long deadline = System.currentTimeMillis() + 20_000L;
            boolean started = false;
            while (System.currentTimeMillis() < deadline) {
                    try {
                    Thread.sleep(500);
                    // probe the shim on the selected port
                    tools.java.EmbedClient portClient = new tools.java.EmbedClient("http://127.0.0.1:" + port);
                            portClient.embedJson("{\"texts\":[\"ping\"]}");
                            // After the shim is reachable, probe /metrics for runtime info and log it for CI debugging
                            try {
                                String metrics = portClient.getMetrics();
                                System.out.println("[embed-shim-metrics] " + metrics);
                                // Parse metrics into strongly-typed POJO and optionally assert expectations
                                try {
                                    MetricsInfo m = MetricsInfo.fromJson(metrics);
                                    // If CI or caller requests the runtime to be loaded, assert it
                                    String requireLoadedProp = System.getProperty("embed.shim.requireLoaded", "false");
                                    boolean requireLoaded = Boolean.parseBoolean(requireLoadedProp);
                                    if (requireLoaded) {
                                        if (!m.loaded) {
                                            throw new IllegalStateException("Embed shim reported not loaded in /metrics but embed.shim.requireLoaded=true");
                                        }
                                    }
                                    // Optional expected runtime (e.g., "st" or "onnx")
                                    String expectRuntime = System.getProperty("embed.shim.expectRuntime", "");
                                    if (expectRuntime != null && !expectRuntime.isBlank()) {
                                        if (m.runtime == null || !m.runtime.equals(expectRuntime)) {
                                            throw new IllegalStateException("Embed shim runtime mismatch: expected=" + expectRuntime + " got=" + m.runtime);
                                        }
                                    }
                                } catch (Exception pe) {
                                    // Parsing or assertion failed - surface for CI debugging
                                    System.out.println("[embed-shim-metrics-parse] " + pe.getMessage());
                                    throw pe;
                                }
                            } catch (Exception me) {
                                System.out.println("[embed-shim-metrics] failed to query /metrics: " + me.getMessage());
                            }
                    this.baseUrl = "http://127.0.0.1:" + port;
                    started = true;
                    break;
                } catch (IOException ignored) {
                }
            }
            if (!started) {
                // kill process and fail
                shimProcess.destroyForcibly();
                shimProcess = null;
                // attach shim log if present and copy to failsafe-reports
                Path logPath = Path.of("target", "failsafe-shim.log");
                try {
                        if (Files.exists(logPath)) {
                        Path destDir = Path.of("target", "failsafe-reports");
                        Files.createDirectories(destDir);
                        Path dest = destDir.resolve("EmbedServiceIntegrationTestIT-shim.log");
                        Files.copy(logPath, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException ignored) {
                }
                throw new IllegalStateException("Failed to start embedding shim for integration test. Check target/failsafe-reports/EmbedServiceIntegrationTestIT-shim.log for details.");
            }
        }
    }

    @AfterEach
    public void cleanupShim() {
        if (shimProcess != null && shimProcess.isAlive()) {
            shimProcess.destroy();
            try {
                shimProcess.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
            if (shimProcess.isAlive()) shimProcess.destroyForcibly();
        }
    }

    @Test
    public void testEmbedEndpointReturns384Vectors() throws Exception {
        // ensureShimRunning sets `baseUrl`
        tools.java.EmbedClient client = new tools.java.EmbedClient(this.baseUrl != null ? this.baseUrl : "http://127.0.0.1:8000");
        String payload = "{\"texts\":[\"hello world\",\"some code snippet\"]}";
        String respBody = client.embedJson(payload);

        assertNotNull(respBody, "Response body should not be null");

        ObjectMapper om = new ObjectMapper();
        JsonNode root = om.readTree(respBody);
        assertTrue(root.has("vectors"), "Response JSON should contain 'vectors'");
        JsonNode vectors = root.get("vectors");
        assertTrue(vectors.isArray(), "vectors should be an array");
        assertTrue(vectors.size() >= 1, "expected at least one vector");

        try {
            for (JsonNode vec : vectors) {
            assertTrue(vec.isArray(), "each vector must be an array");
            assertEquals(EXPECTED_DIM, vec.size(), "each vector should have dimension " + EXPECTED_DIM);
            double sumSq = 0.0;
            for (JsonNode n : vec) {
                double v = n.asDouble();
                sumSq += v * v;
            }
            assertTrue(sumSq > 1e-12, "vector appears to be all zeros");
        }
        } catch (AssertionError ae) {
            // Copy shim log into failsafe-reports for CI debugging
            Path logPath = Path.of("target", "failsafe-shim.log");
            try {
                if (Files.exists(logPath)) {
                    Path destDir = Path.of("target", "failsafe-reports");
                    Files.createDirectories(destDir);
                    Path dest = destDir.resolve("EmbedServiceIntegrationTestIT-shim.log");
                    Files.copy(logPath, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    String logs = Files.readString(dest);
                    throw new AssertionError(ae.getMessage() + "\n---- shim log (tail 2000 chars) ----\n" +
                            (logs.length() > 2000 ? logs.substring(logs.length() - 2000) : logs), ae);
                }
            } catch (IOException ignored) {
            }
            throw ae;
        }
    }
}

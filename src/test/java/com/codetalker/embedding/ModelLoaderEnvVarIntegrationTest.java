package com.codetalker.embedding;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class ModelLoaderEnvVarIntegrationTest {

    @SuppressWarnings({"unchecked","rawtypes"})
    private void setEnv(String key, String value) throws Exception {
        try {
            Map<String, String> newenv = new HashMap<>();
            newenv.put(key, value);
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        } catch (NoSuchFieldException nsf) {
            // fallback for other JVMs
            Class[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for (Class cl : classes) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field m = cl.getDeclaredField("m");
                    m.setAccessible(true);
                    Object obj = m.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.put(key, value);
                }
            }
        }
    }

    @Test
    void testModelLoaderUsesEnvVar() throws Exception {
        Path tmp = Files.createTempDirectory("codetalker-model-env");
        Path f = tmp.resolve("dummy-model.bin");
        Files.writeString(f, "dummy");

        // Set env var in-process for the test
        setEnv("CODE_TALKER_MODEL_PATH", tmp.toString());

        // Now instantiate default ModelLoader which should pick up the env var
        ModelLoader loader = new ModelLoader();
        loader.load();
        assertTrue(loader.isModelLoaded(), "ModelLoader should report loaded when CODE_TALKER_MODEL_PATH is set");

        // cleanup
        Files.deleteIfExists(f);
        Files.deleteIfExists(tmp);
    }
}

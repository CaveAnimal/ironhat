package com.codetalker.testing;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MetricsInfo {
    public final String runtime; // "st" | "onnx" | null
    public final boolean loaded;
    public final String error;
    public final Map<String, String> versions;

    public MetricsInfo(String runtime, boolean loaded, String error, Map<String, String> versions) {
        this.runtime = runtime;
        this.loaded = loaded;
        this.error = error;
        this.versions = versions;
    }

    public static MetricsInfo fromJson(String json) throws Exception {
        ObjectMapper om = new ObjectMapper();
        JsonNode root = om.readTree(json);
        String runtime = null;
        if (root.has("runtime") && !root.get("runtime").isNull()) {
            runtime = root.get("runtime").asText();
        }
        boolean loaded = root.has("loaded") && root.get("loaded").asBoolean(false);
        String error = null;
        if (root.has("error") && !root.get("error").isNull()) {
            error = root.get("error").asText();
        }
        Map<String, String> versions = null;
        if (root.has("versions") && root.get("versions").isObject()) {
            @SuppressWarnings("unchecked")
            Map<String, String> tmp = (Map<String, String>) om.convertValue(root.get("versions"), Map.class);
            versions = tmp;
        }
        return new MetricsInfo(runtime, loaded, error, versions);
    }
}

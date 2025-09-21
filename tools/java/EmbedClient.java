package tools.java;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class EmbedClient {
    private final HttpClient http;
    private final URI endpoint;

    public EmbedClient(String baseUrl) {
        this.http = HttpClient.newHttpClient();
        this.endpoint = URI.create(baseUrl + "/embed");
    }

    /**
     * POSTs the given JSON body to /embed and returns the raw response body as a string.
     */
    public String embedJson(String jsonPayload) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(endpoint)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new IOException("Embedding service returned status " + resp.statusCode() + ": " + resp.body());
        }
        return resp.body();
    }
}
